package implementsample.model;

import javax.validation.constraints.NotEmpty;

public class UserDeleteRequest {

    @NotEmpty(message = "Tenant ID is required")
    private String tenantId;

    @NotEmpty(message = "User ID is required")
    private String userId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
