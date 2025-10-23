package implementsample.model;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TenantPlanResponse {
    
    private String id;
    private String name;
    
    @JsonProperty("plan_id")
    private String planId;
    
    @JsonProperty("tax_rate_id")
    private String taxRateId;
    
    @JsonProperty("plan_reservation")
    private Map<String, Object> planReservation;
    
    public TenantPlanResponse() {}
    
    public TenantPlanResponse(String id, String name, String planId, String taxRateId, Map<String, Object> planReservation) {
        this.id = id;
        this.name = name;
        this.planId = planId;
        this.taxRateId = taxRateId;
        this.planReservation = planReservation;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPlanId() {
        return planId;
    }
    
    public void setPlanId(String planId) {
        this.planId = planId;
    }
    
    public String getTaxRateId() {
        return taxRateId;
    }
    
    public void setTaxRateId(String taxRateId) {
        this.taxRateId = taxRateId;
    }
    
    public Map<String, Object> getPlanReservation() {
        return planReservation;
    }
    
    public void setPlanReservation(Map<String, Object> planReservation) {
        this.planReservation = planReservation;
    }
}
