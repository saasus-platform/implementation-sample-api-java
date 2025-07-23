package implementsample.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class UpdateMeteringCountRequest {

    @NotNull(message = "Method is required")
    @Pattern(regexp = "^(add|sub|direct)$", message = "Method must be one of: add, sub, direct")
    private String method;

    @Min(value = 0, message = "Count must be greater than or equal to 0")
    private int count;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
