package implementsample.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.Map;

public class UserInvitationRequest {

    @NotEmpty(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotEmpty(message = "Tenant ID is required")
    private String tenantId;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
