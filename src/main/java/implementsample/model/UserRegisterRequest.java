package implementsample.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.Map;

public class UserRegisterRequest {

    @NotEmpty(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotEmpty(message = "Password is required")
    private String password;

    @NotEmpty(message = "Tenant ID is required")
    private String tenantId;

    private Map<String, Object> userAttributeValues;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getUserAttributeValues() {
        return userAttributeValues;
    }

    public void setUserAttributeValues(Map<String, Object> userAttributeValues) {
        this.userAttributeValues = userAttributeValues;
    }
}
