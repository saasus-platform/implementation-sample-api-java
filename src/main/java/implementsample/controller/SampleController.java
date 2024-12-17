package implementsample.controller;

import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import saasus.sdk.auth.ApiException;
import saasus.sdk.auth.api.CredentialApi;
import saasus.sdk.auth.api.TenantApi;
import saasus.sdk.auth.api.TenantUserApi;
import saasus.sdk.auth.api.TenantAttributeApi;
import saasus.sdk.auth.api.UserInfoApi;
import saasus.sdk.auth.api.UserAttributeApi;
import saasus.sdk.pricing.api.PricingPlansApi;
import saasus.sdk.auth.models.Credentials;
import saasus.sdk.auth.models.UserInfo;
import saasus.sdk.auth.models.Users;
import saasus.sdk.modules.AuthApiClient;
import saasus.sdk.modules.PricingApiClient;
import saasus.sdk.modules.Configuration;
import saasus.sdk.auth.models.TenantAttributes;
import saasus.sdk.auth.models.TenantDetail;
import saasus.sdk.auth.models.Attribute;
import saasus.sdk.auth.models.UserAttributes;
import saasus.sdk.pricing.models.PricingPlan;

@RestController
public class SampleController {
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
    public ResponseEntity<?> getUsers(HttpSession session, HttpServletRequest request) throws Exception {
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

        String tenantId = userInfo.getTenants().get(0).getId();
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

        return ResponseEntity.ok(userAttributes);
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

        System.out.println("=======================planId");
        System.out.println(planId);
        System.out.println("=======================planIdEnd");
        if (planId == null || planId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No price plan found for the tenant");
        }

        System.out.println("1");
        PricingApiClient pricingApiClient = new Configuration().getPricingApiClient();
        System.out.println("2");
        pricingApiClient.setReferer(request.getHeader("Referer"));
        System.out.println("3");
        PricingPlansApi pricingPlansApi = new PricingPlansApi(pricingApiClient);
        System.out.println("4");
        PricingPlan pricingPlan = null;
        System.out.println("5");
        try {
            pricingPlan = pricingPlansApi.getPricingPlan(planId);
            System.out.println("6");
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

        System.out.println("7");
        return ResponseEntity.ok(pricingPlan);
    }
}
