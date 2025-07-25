package implementsample.model;

import java.util.List;
import java.util.Map;

public class BillingCalculationResult {
  private final List<Map<String, Object>> billings;
  private final List<Map<String, Object>> totals;

  public BillingCalculationResult(List<Map<String, Object>> billings, List<Map<String, Object>> totals) {
    this.billings = billings;
    this.totals = totals;
  }

  public List<Map<String, Object>> getBillings() {
    return billings;
  }

  public List<Map<String, Object>> getTotals() {
    return totals;
  }
}
