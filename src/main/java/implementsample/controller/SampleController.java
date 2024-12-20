package implementsample.controller;

import java.util.StringTokenizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import saasus.sdk.auth.ApiException;
import saasus.sdk.auth.api.CredentialApi;
import saasus.sdk.auth.api.RoleApi;
import saasus.sdk.auth.api.SaasUserApi;
import saasus.sdk.auth.api.TenantApi;
import saasus.sdk.auth.api.TenantUserApi;
import saasus.sdk.auth.api.TenantAttributeApi;
import saasus.sdk.auth.api.UserInfoApi;
import saasus.sdk.auth.api.UserAttributeApi;
import saasus.sdk.pricing.api.PricingPlansApi;
import saasus.sdk.auth.models.Credentials;
import saasus.sdk.auth.models.Roles;
import saasus.sdk.auth.models.UserInfo;
import saasus.sdk.auth.models.Users;
import saasus.sdk.modules.AuthApiClient;
import saasus.sdk.modules.PricingApiClient;
import saasus.sdk.modules.Configuration;
import saasus.sdk.auth.models.TenantAttributes;
import saasus.sdk.auth.models.TenantDetail;
import saasus.sdk.auth.models.User;
import saasus.sdk.auth.models.Attribute;
import saasus.sdk.auth.models.CreateSaasUserParam;
import saasus.sdk.auth.models.CreateTenantUserParam;
import saasus.sdk.auth.models.CreateTenantUserRolesParam;
import saasus.sdk.auth.models.UserAttributes;
import saasus.sdk.pricing.models.PricingPlan;

import implementsample.entity.DeleteUserLog;
import implementsample.model.UserDeleteRequest;
import implementsample.model.UserRegisterRequest;
import implementsample.repository.DeleteUserLogRepository;


@RestController
public class SampleController {

    private final DeleteUserLogRepository deleteUserLogRepository;

    // コンストラクタで依存性注入
    public SampleController(DeleteUserLogRepository deleteUserLogRepository) {
        this.deleteUserLogRepository = deleteUserLogRepository;
    }

    public static String getIDToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.countTokens() == 2 && st.nextToken().equalsIgnoreCase("Bearer")) {
                return st.nextToken();
            }
        }
        return "";
    }

    @GetMapping(value = "/credentials", produces = "application/json")
    public ResponseEntity<?> getCredentials(HttpSession session, HttpServletRequest request) throws Exception {
        String code = request.getParameter("code");

        AuthApiClient apiClient = new Configuration().getAuthApiClient();
        apiClient.setReferer(request.getHeader("Referer"));
        CredentialApi apiInstance = new CredentialApi(apiClient);
        Credentials result = null;
        try {
            result = apiInstance.getAuthCredentials(code, "tempCodeAuth", null);
        } catch (ApiException e) {
            System.err.println("Exception when calling CredentialApi#getAuthCredentials");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        return ResponseEntity.ok(result.toJson());
    }

    @GetMapping(value = "/refresh", produces = "application/json")
    public ResponseEntity<?> refresh(HttpSession session, HttpServletRequest request,
            @CookieValue(name = "SaaSusRefreshToken", defaultValue = "") String cookieValue) throws Exception {
        if (cookieValue == "") {
            return ResponseEntity.badRequest().body("No refresh token found");
        }

        AuthApiClient apiClient = new Configuration().getAuthApiClient();
        apiClient.setReferer(request.getHeader("Referer"));
        CredentialApi apiInstance = new CredentialApi(apiClient);
        Credentials result = null;
        try {
            result = apiInstance.getAuthCredentials(null, "refreshTokenAuth", cookieValue);
        } catch (ApiException e) {
            System.err.println("Exception when calling CredentialApi#getAuthCredentials");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }
        return ResponseEntity.ok(result.toJson());
    }

    @GetMapping(value = "/userinfo", produces = "application/json")
    public ResponseEntity<?> getMe(HttpSession session, HttpServletRequest request) throws Exception {
        AuthApiClient apiClient = new Configuration().getAuthApiClient();
        apiClient.setReferer(request.getHeader("Referer"));

        UserInfoApi userInfoApi = new UserInfoApi(apiClient);
        UserInfo userInfo = null;
        try {
            userInfo = userInfoApi.getUserInfo(getIDToken(request));
            System.out.println(userInfo);
        } catch (ApiException e) {
            System.err.println("Exception when calling UserInfoApi#getUserInfo");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        System.out.println(userInfo.toJson());
        return ResponseEntity.ok(userInfo.toJson());
    }

    @GetMapping(value = "/users", produces = "application/json")
    public ResponseEntity<?> getUsers(HttpSession session, HttpServletRequest request, @RequestParam(value = "tenant_id", required = false) String tenantId) throws Exception {
        AuthApiClient apiClient = new Configuration().getAuthApiClient();
        apiClient.setReferer(request.getHeader("Referer"));

        UserInfoApi userInfoApi = new UserInfoApi(apiClient);
        UserInfo userInfo = null;
        try {
            userInfo = userInfoApi.getUserInfo(getIDToken(request));
            System.out.println(userInfo);
        } catch (ApiException e) {
            System.err.println("Exception when calling UserInfoApi#getUserInfo");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        if (userInfo.getTenants() == null || userInfo.getTenants().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tenants found for the user");
        }
        System.out.println(userInfo.getTenants());
        
        if (tenantId == null || tenantId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenant_id query parameter is missing");
        }
        boolean isBelongingTenant = userInfo.getTenants().stream()
        .anyMatch(tenant -> tenant.getId().equals(tenantId));
        if (!isBelongingTenant) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Tenant that does not belong");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        TenantUserApi tenantUserApi = new TenantUserApi(apiClient);
        Users users = null;
        try {
            users = tenantUserApi.getTenantUsers(tenantId);
            System.out.println(users);
        } catch (ApiException e) {
            System.err.println("Exception when calling TenantUserApi#getTenantUsers");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        return ResponseEntity.ok(users.getUsers());
    }

    @GetMapping(value = "/tenant_attributes", produces = "application/json")
    public ResponseEntity<?> getTenantAttributes(HttpSession session, HttpServletRequest request) throws Exception {
        String tenantId = request.getParameter("tenant_id");
        AuthApiClient apiClient = new Configuration().getAuthApiClient();
        apiClient.setReferer(request.getHeader("Referer"));

        UserInfoApi userInfoApi = new UserInfoApi(apiClient);
        UserInfo userInfo = null;
        try {
            userInfo = userInfoApi.getUserInfo(getIDToken(request));
            System.out.println(userInfo);
        } catch (ApiException e) {
            System.err.println("Exception when calling UserInfoApi#getUserInfo");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }
        System.out.println(userInfo.getTenants());

        TenantAttributeApi tenantAttributeApi = new TenantAttributeApi(apiClient);
        TenantAttributes tenantAttributes = null;
        try {
            tenantAttributes = tenantAttributeApi.getTenantAttributes();
            System.out.println(tenantAttributes);
        } catch (ApiException e) {
            System.err.println("Exception when calling TenantAttributeApi#getTenantAttributes");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        TenantApi tenantApi = new TenantApi(apiClient);
        TenantDetail tenantDetail = null;
        try {
            tenantDetail = tenantApi.getTenant(tenantId);
            System.out.println(tenantDetail);
        } catch (ApiException e) {
            System.err.println("Exception when calling TenantApi#getTenant");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        Map<String, Map<String, Object>> details = new HashMap<>();
        for (Attribute attribute : tenantAttributes.getTenantAttributes()) {
            String attributeName = attribute.getAttributeName();
            Map<String, Object> detail = new HashMap<>();
            detail.put("display_name", attribute.getDisplayName());
            detail.put("attribute_type", attribute.getAttributeType());
            detail.put("value", tenantDetail.getAttributes().get(attributeName));

            details.put(attributeName, detail);
        }
        System.out.println(details);

        return ResponseEntity.ok(details);
    }

    @GetMapping(value = "/user_attributes", produces = "application/json")
    public ResponseEntity<?> getUserAttributes(HttpSession session, HttpServletRequest request) throws Exception {
        AuthApiClient apiClient = new Configuration().getAuthApiClient();
        apiClient.setReferer(request.getHeader("Referer"));

        UserAttributeApi userAttributeApi = new UserAttributeApi(apiClient);
        UserAttributes userAttributes = null;
        try {
            userAttributes = userAttributeApi.getUserAttributes();
            System.out.println(userAttributes);
        } catch (ApiException e) {
            System.err.println("Exception when calling UserAttributeApi#getUserAttributes");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        return ResponseEntity.ok(userAttributes.toJson());
    }

    @GetMapping(value = "/pricing_plan", produces = "application/json")
    public ResponseEntity<?> getPricingPlan(HttpSession session, HttpServletRequest request, @RequestParam(value = "plan_id", required = false) String planId) throws Exception {
        AuthApiClient apiClient = new Configuration().getAuthApiClient();
        apiClient.setReferer(request.getHeader("Referer"));

        UserInfoApi userInfoApi = new UserInfoApi(apiClient);
        UserInfo userInfo = null;
        try {
            userInfo = userInfoApi.getUserInfo(getIDToken(request));
            System.out.println(userInfo);
        } catch (ApiException e) {
            System.err.println("Exception when calling UserInfoApi#getUserInfo");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        if (userInfo.getTenants() == null || userInfo.getTenants().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tenants found for the user");
        }

        if (planId == null || planId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No price plan found for the tenant");
        }

        PricingApiClient pricingApiClient = new Configuration().getPricingApiClient();
        pricingApiClient.setReferer(request.getHeader("Referer"));
        PricingPlansApi pricingPlansApi = new PricingPlansApi(pricingApiClient);
        PricingPlan pricingPlan = null;
        try {
            pricingPlan = pricingPlansApi.getPricingPlan(planId);
            System.out.println(pricingPlan);
        } catch (saasus.sdk.pricing.ApiException e) {
            System.out.println("Error");
            System.out.println("Exception when calling PricingPlansApi#getPricingPlan");
            System.out.println("Status code: " + e.getCode());
            System.out.println("Reason: " + e.getResponseBody());
            System.out.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }

        return ResponseEntity.ok(pricingPlan.toJson());
    }

    @PostMapping(value = "/user_register", produces = "application/json")
    public ResponseEntity<?> userRegister(HttpServletRequest request, @Valid @RequestBody UserRegisterRequest requestBody) {
        System.out.println("API Request: user_register started");
        try {
            String email = requestBody.getEmail();
            String password = requestBody.getPassword();
            String tenantId = requestBody.getTenantId();

            AuthApiClient apiClient = new Configuration().getAuthApiClient();
            apiClient.setReferer(request.getHeader("Referer"));

            System.out.println("Making API call: getUserInfo");
            UserInfoApi userInfoApi = new UserInfoApi(apiClient);
            UserInfo userInfo = userInfoApi.getUserInfo(getIDToken(request));

            if (userInfo.getTenants() == null || userInfo.getTenants().isEmpty()) {
                System.err.println("No tenants found for the user");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tenants found for the user");
            }

            boolean isBelongingTenant = userInfo.getTenants().stream()
                    .anyMatch(tenant -> tenant.getId().equals(tenantId));
            if (!isBelongingTenant) {
                System.err.println("Tenant that does not belong: " + tenantId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant that does not belong");
            }

            System.out.println("Making API call: getUserAttributes");
            UserAttributeApi userAttributeApi = new UserAttributeApi(apiClient);
            UserAttributes userAttributes = userAttributeApi.getUserAttributes();

            Map<String, Object> userAttributeValues = new HashMap<>();
            Map<String, Object> requestUserAttributes = requestBody.getUserAttributeValues() != null
                    ? requestBody.getUserAttributeValues()
                    : new HashMap<>();

            for (Attribute attribute : userAttributes.getUserAttributes()) {
                String attributeName = attribute.getAttributeName();
                String attributeType = attribute.getAttributeType().getValue();

                if (requestUserAttributes.containsKey(attributeName)) {
                    Object attributeValue = requestUserAttributes.get(attributeName);

                    if ("number".equalsIgnoreCase(attributeType)) {
                        try {
                            int numericValue = Integer.parseInt(attributeValue.toString());
                            userAttributeValues.put(attributeName, numericValue);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid value for attribute: " + attributeName);
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value for attribute: " + attributeName);
                        }
                    } else {
                        userAttributeValues.put(attributeName, attributeValue);
                    }
                }
            }

            System.out.println("Making API call: createSaasUser");
            SaasUserApi saasUserApi = new SaasUserApi(apiClient);
            CreateSaasUserParam createSaasUserParam = new CreateSaasUserParam().email(email).password(password);
            saasUserApi.createSaasUser(createSaasUserParam);

            System.out.println("Making API call: createTenantUser");
            TenantUserApi tenantUserApi = new TenantUserApi(apiClient);
            CreateTenantUserParam createTenantUserParam = new CreateTenantUserParam()
                    .email(email)
                    .attributes(userAttributeValues);
            User tenantUser = tenantUserApi.createTenantUser(tenantId, createTenantUserParam);

            System.out.println("Making API call: getRoles");
            RoleApi roleApi = new RoleApi(apiClient);
            Roles roles = roleApi.getRoles();
            String addRole = roles.getRoles().stream().anyMatch(role -> "user".equals(role.getRoleName())) ? "user" : "admin";

            System.out.println("Making API call: createTenantUserRoles");
            CreateTenantUserRolesParam createTenantUserRolesParam = new CreateTenantUserRolesParam();
            createTenantUserRolesParam.setRoleNames(Arrays.asList(addRole));
            tenantUserApi.createTenantUserRoles(tenantId, tenantUser.getId(), 3, createTenantUserRolesParam);

            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", "User registered successfully");
            return ResponseEntity.ok(successResponse);

        } catch (ApiException e) {
            System.err.println("API Exception: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "API Exception: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected Exception: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Exception: " + e.getMessage(), e);
        }
    }

    @DeleteMapping(value = "/user_delete", produces = "application/json")
    public ResponseEntity<?> userDelete(@Valid @RequestBody UserDeleteRequest requestBody, HttpServletRequest request) {
        System.out.println("API Request: user_delete started");
        try {
            String tenantId = requestBody.getTenantId();
            String userId = requestBody.getUserId();

            AuthApiClient apiClient = new Configuration().getAuthApiClient();
            apiClient.setReferer(request.getHeader("Referer"));

            System.out.println("Making API call: getUserInfo");
            UserInfoApi userInfoApi = new UserInfoApi(apiClient);
            UserInfo userInfo = userInfoApi.getUserInfo(getIDToken(request));

            if (userInfo.getTenants() == null || userInfo.getTenants().isEmpty()) {
                System.err.println("No tenants found for the user");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tenants found for the user");
            }

            boolean isBelongingTenant = userInfo.getTenants().stream()
                    .anyMatch(tenant -> tenant.getId().equals(tenantId));
            if (!isBelongingTenant) {
                System.err.println("Tenant that does not belong: " + tenantId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant that does not belong");
            }

            System.out.println("Making API call: getTenantUser");
            TenantUserApi tenantUserApi = new TenantUserApi(apiClient);
            User deleteUser = tenantUserApi.getTenantUser(tenantId, userId);

            System.out.println("Making API call: deleteTenantUser");
            tenantUserApi.deleteTenantUser(tenantId, userId);
            System.out.println("Tenant user deleted successfully");

            System.out.println("Logging user deletion");
            DeleteUserLog deleteLog = new DeleteUserLog();
            deleteLog.setTenantId(tenantId);
            deleteLog.setUserId(userId);
            deleteLog.setEmail(deleteUser.getEmail());
            deleteUserLogRepository.save(deleteLog);
            System.out.println("User deletion logged successfully");

            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("message", "User deleted successfully.");
            return ResponseEntity.ok(successResponse);

        } catch (ApiException e) {
            System.err.println("API Exception: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "API Exception: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected Exception: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Exception: " + e.getMessage(), e);
        }
    }

    @GetMapping(value = "/delete_user_log", produces = "application/json")
    public ResponseEntity<?> getDeleteLogs(@RequestParam("tenant_id") String tenantId, HttpServletRequest request) {
        try {
            AuthApiClient apiClient = new Configuration().getAuthApiClient();
            apiClient.setReferer(request.getHeader("Referer"));

            UserInfoApi userInfoApi = new UserInfoApi(apiClient);
            UserInfo userInfo = userInfoApi.getUserInfo(getIDToken(request));

            if (userInfo.getTenants() == null || userInfo.getTenants().isEmpty()) {
                System.err.println("No tenants found for the user");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tenants found for the user");
            }

            boolean isBelongingTenant = userInfo.getTenants().stream()
                    .anyMatch(tenant -> tenant.getId().equals(tenantId));
            if (!isBelongingTenant) {
                System.err.println("Tenant that does not belong: " + tenantId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant that does not belong");
            }

            List<DeleteUserLog> logs = deleteUserLogRepository.findByTenantId(tenantId);

            List<Map<String, Object>> response = new ArrayList<>();
            for (DeleteUserLog log : logs) {
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("id", log.getId());
                logEntry.put("tenant_id", log.getTenantId());
                logEntry.put("user_id", log.getUserId());
                logEntry.put("email", log.getEmail());
                // ISO 8601フォーマットでdelete_atを設定
                if (log.getDeleteAt() != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                    String formattedDate = log.getDeleteAt().atZone(ZoneId.systemDefault()).format(formatter);
                    logEntry.put("delete_at", formattedDate);
                } else {
                    logEntry.put("delete_at", null);
                }
                response.add(logEntry);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Unexpected Exception: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Exception: " + e.getMessage(), e);
        }
    }
}
