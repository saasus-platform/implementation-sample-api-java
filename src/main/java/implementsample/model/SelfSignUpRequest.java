package implementsample.model;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

public class SelfSignUpRequest {

    @NotEmpty(message = "Tenant name is required")
    private String tenantName;

    private Map<String, Object> tenantAttributeValues;
    private Map<String, Object> userAttributeValues;

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Map<String, Object> getTenantAttributeValues() {
        return tenantAttributeValues;
    }

    public void setTenantAttributeValues(Map<String, Object> tenantAttributeValues) {
        this.tenantAttributeValues = tenantAttributeValues;
    }

    public Map<String, Object> getUserAttributeValues() {
        return userAttributeValues;
    }

    public void setUserAttributeValues(Map<String, Object> userAttributeValues) {
        this.userAttributeValues = userAttributeValues;
    }
}
