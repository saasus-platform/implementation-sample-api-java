package implementsample.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateTenantPlanRequest {
    
    @JsonProperty("next_plan_id")
    private String nextPlanId;
    
    @JsonProperty("tax_rate_id")
    private String taxRateId;
    
    @JsonProperty("using_next_plan_from")
    private Long usingNextPlanFrom;
    
    public UpdateTenantPlanRequest() {}
    
    public UpdateTenantPlanRequest(String nextPlanId, String taxRateId, Long usingNextPlanFrom) {
        this.nextPlanId = nextPlanId;
        this.taxRateId = taxRateId;
        this.usingNextPlanFrom = usingNextPlanFrom;
    }
    
    public String getNextPlanId() {
        return nextPlanId;
    }
    
    public void setNextPlanId(String nextPlanId) {
        this.nextPlanId = nextPlanId;
    }
    
    public String getTaxRateId() {
        return taxRateId;
    }
    
    public void setTaxRateId(String taxRateId) {
        this.taxRateId = taxRateId;
    }
    
    public Long getUsingNextPlanFrom() {
        return usingNextPlanFrom;
    }
    
    public void setUsingNextPlanFrom(Long usingNextPlanFrom) {
        this.usingNextPlanFrom = usingNextPlanFrom;
    }
}
