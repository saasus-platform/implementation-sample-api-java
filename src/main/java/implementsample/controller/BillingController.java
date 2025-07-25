package implementsample.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import saasus.sdk.auth.ApiException;
import saasus.sdk.auth.api.TenantApi;
import saasus.sdk.auth.api.UserInfoApi;
import saasus.sdk.auth.models.*;
import saasus.sdk.modules.AuthApiClient;
import saasus.sdk.modules.PricingApiClient;
import saasus.sdk.modules.Configuration;
import saasus.sdk.pricing.api.MeteringApi;
import saasus.sdk.pricing.api.PricingPlansApi;
import saasus.sdk.pricing.api.TaxRateApi;
import saasus.sdk.pricing.models.AggregateUsage;
import saasus.sdk.pricing.models.MeteringUnitCount;
import saasus.sdk.pricing.models.MeteringUnitDatePeriodCounts;
import saasus.sdk.pricing.models.MeteringUnitTimestampCount;
import saasus.sdk.pricing.models.PricingFixedUnit;
import saasus.sdk.pricing.models.PricingMenu;
import saasus.sdk.pricing.models.PricingPlan;
import saasus.sdk.pricing.models.PricingTier;
import saasus.sdk.pricing.models.PricingUsageUnit;
import saasus.sdk.pricing.models.PricingTieredUsageUnit;
import saasus.sdk.pricing.models.PricingUnit;
import saasus.sdk.pricing.models.PricingTieredUnit;
import saasus.sdk.pricing.models.RecurringInterval;
import saasus.sdk.pricing.models.TaxRate;
import saasus.sdk.pricing.models.UpdateMeteringUnitTimestampCountParam;
import saasus.sdk.pricing.models.UpdateMeteringUnitTimestampCountMethod;
import saasus.sdk.pricing.models.UpdateMeteringUnitTimestampCountNowParam;
import implementsample.model.BillingCalculationResult;
import implementsample.model.UpdateMeteringCountRequest;

@RestController
public class BillingController {

  private String getIDToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return "";
  }

  private boolean hasBillingAccess(UserInfo userInfo, String tenantId) {
    if (userInfo.getTenants() == null || userInfo.getTenants().isEmpty())
      return false;
    for (UserAvailableTenant t : userInfo.getTenants()) {
      if (!t.getId().equals(tenantId))
        continue;
      for (UserAvailableEnv env : t.getEnvs()) {
        for (Role role : env.getRoles()) {
          if ("admin".equals(role.getRoleName()) || "sadmin".equals(role.getRoleName())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private BillingCalculationResult calcMeteringUnitBillings(
      MeteringApi meteringApi,
      PricingPlan plan,
      String tenantId,
      int start,
      int end) throws ApiException, saasus.sdk.pricing.ApiException {

    List<Map<String, Object>> billings = new ArrayList<>();
    Map<String, Double> currencySum = new HashMap<>();
    Map<String, Double> usageCache = new HashMap<>();

    for (PricingMenu menu : plan.getPricingMenus()) {
      for (PricingUnit unitObj : menu.getUnits()) {
        Object instance = unitObj.getActualInstance();
        if (!(instance instanceof PricingFixedUnit
            || instance instanceof PricingUsageUnit
            || instance instanceof PricingTieredUnit
            || instance instanceof PricingTieredUsageUnit)) {
          continue;
        }

        String unitName = null;
        String unitType = null;
        String agg = "sum";
        String currency = "JPY";
        String dispName = null;

        if (instance instanceof PricingFixedUnit) {
          PricingFixedUnit unit = (PricingFixedUnit) instance;
          unitType = unit.getType() != null ? unit.getType().getValue() : "fixed";
          currency = unit.getCurrency() != null ? unit.getCurrency().getValue() : "JPY";
          dispName = unit.getDisplayName();
        } else if (instance instanceof PricingUsageUnit) {
          PricingUsageUnit unit = (PricingUsageUnit) instance;
          unitName = unit.getMeteringUnitName();
          unitType = unit.getType() != null ? unit.getType().getValue() : "usage";
          AggregateUsage aggregateUsage = unit.getAggregateUsage();
          agg = aggregateUsage != null ? aggregateUsage.getValue() : "sum";
          currency = unit.getCurrency() != null ? unit.getCurrency().getValue() : "JPY";
          dispName = unit.getDisplayName();
        } else if (instance instanceof PricingTieredUnit) {
          PricingTieredUnit unit = (PricingTieredUnit) instance;
          unitName = unit.getMeteringUnitName();
          unitType = unit.getType() != null ? unit.getType().getValue() : "tiered";
          AggregateUsage aggregateUsage = unit.getAggregateUsage();
          agg = aggregateUsage != null ? aggregateUsage.getValue() : "sum";
          currency = unit.getCurrency() != null ? unit.getCurrency().getValue() : "JPY";
          dispName = unit.getDisplayName();
        } else if (instance instanceof PricingTieredUsageUnit) {
          PricingTieredUsageUnit unit = (PricingTieredUsageUnit) instance;
          unitName = unit.getMeteringUnitName();
          unitType = unit.getType() != null ? unit.getType().getValue() : "tiered_usage";
          AggregateUsage aggregateUsage = unit.getAggregateUsage();
          agg = aggregateUsage != null ? aggregateUsage.getValue() : "sum";
          currency = unit.getCurrency() != null ? unit.getCurrency().getValue() : "JPY";
          dispName = unit.getDisplayName();
        }

        double count = 0.0;
        if (!"fixed".equals(unitType)) {
          if (usageCache.containsKey(unitName)) {
            count = usageCache.get(unitName);
          } else {
            MeteringUnitDatePeriodCounts counts = meteringApi
                .getMeteringUnitDateCountByTenantIdAndUnitNameAndDatePeriod(tenantId, unitName, start, end);
            if ("max".equalsIgnoreCase(agg)) {
              for (MeteringUnitCount c : counts.getCounts()) {
                count = Math.max(count, c.getCount());
              }
            } else {
              for (MeteringUnitCount c : counts.getCounts()) {
                count += c.getCount();
              }
            }
            usageCache.put(unitName, count);
          }
        }

        double amount = calculateAmountByUnitType(count, instance);
        currencySum.put(currency, currencySum.getOrDefault(currency, 0.0) + amount);

        Map<String, Object> entry = new HashMap<>();
        entry.put("metering_unit_name", unitName);
        entry.put("metering_unit_type", unitType);
        entry.put("function_menu_name", menu.getDisplayName());
        entry.put("period_count", count);
        entry.put("currency", currency);
        entry.put("period_amount", amount);
        entry.put("pricing_unit_display_name", dispName);
        billings.add(entry);
      }
    }

    List<Map<String, Object>> totals = new ArrayList<>();
    for (Map.Entry<String, Double> e : currencySum.entrySet()) {
      Map<String, Object> total = new HashMap<>();
      total.put("currency", e.getKey());
      total.put("total_amount", e.getValue());
      totals.add(total);
    }

    return new BillingCalculationResult(billings, totals);
  }

  private double calculateAmountByUnitType(double count, Object unit) {
    if (unit instanceof PricingFixedUnit) {
      return ((PricingFixedUnit) unit).getUnitAmount();
    } else if (unit instanceof PricingUsageUnit) {
      return ((PricingUsageUnit) unit).getUnitAmount() * count;
    } else if (unit instanceof PricingTieredUnit) {
      return calcTiered(count, (PricingTieredUnit) unit);
    } else if (unit instanceof PricingTieredUsageUnit) {
      return calcTieredUsage(count, (PricingTieredUsageUnit) unit);
    }
    return 0.0;
  }

  private double calcTiered(double count, PricingTieredUnit unit) {
    List<PricingTier> tiers = unit.getTiers();
    if (tiers == null || tiers.isEmpty()) {
      return 0.0;
    }

    for (PricingTier tier : tiers) {
      boolean inf = Boolean.TRUE.equals(tier.getInf());
      int upTo = tier.getUpTo() != null ? tier.getUpTo() : Integer.MAX_VALUE;
      double flat = tier.getFlatAmount() != null ? tier.getFlatAmount() : 0.0;
      double price = tier.getUnitAmount() != null ? tier.getUnitAmount() : 0.0;

      if (inf || count <= upTo) {
        return flat + count * price;
      }
    }

    return 0.0;
  }

  private double calcTieredUsage(double count, PricingTieredUsageUnit unit) {
    List<PricingTier> tiers = unit.getTiers();
    if (tiers == null || tiers.isEmpty()) {
      return 0.0;
    }

    double total = 0.0;
    double prev = 0.0;

    for (PricingTier tier : tiers) {
      boolean inf = Boolean.TRUE.equals(tier.getInf());
      int upTo = tier.getUpTo() != null ? tier.getUpTo() : Integer.MAX_VALUE;
      double flat = tier.getFlatAmount() != null ? tier.getFlatAmount() : 0.0;
      double price = tier.getUnitAmount() != null ? tier.getUnitAmount() : 0.0;

      if (count <= prev) {
        break;
      }

      double usage = inf ? (count - prev) : Math.min(count, upTo) - prev;
      total += flat + usage * price;
      prev = upTo;

      if (inf) {
        break;
      }
    }

    return total;
  }

  @GetMapping(value = "/billing/dashboard", produces = "application/json")
  public ResponseEntity<?> getBillingDashboard(HttpServletRequest request,
      @RequestParam String tenant_id,
      @RequestParam String plan_id,
      @RequestParam("period_start") Integer start,
      @RequestParam("period_end") Integer end) {
    try {
      // =============================================================
      // 1. クライアント初期化（Auth / Pricing）
      // =============================================================
      AuthApiClient authClient = new Configuration().getAuthApiClient();
      authClient.setReferer(request.getHeader("X-Saasus-Referer"));
      PricingApiClient pricingClient = new Configuration().getPricingApiClient();
      pricingClient.setReferer(request.getHeader("X-Saasus-Referer"));

      // =============================================================
      // 2. 認証ユーザー情報取得＆課金閲覧権限チェック
      // - admin / sadmin かつ対象テナントに所属している必要あり
      // =============================================================
      UserInfoApi userInfoApi = new UserInfoApi(authClient);
      UserInfo userInfo = userInfoApi.getUserInfo(getIDToken(request));

      if (!hasBillingAccess(userInfo, tenant_id)) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Insufficient permissions");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
      }

      // =============================================================
      // 3. プラン情報取得（plan_id が無効な場合は 404 ）
      // =============================================================
      PricingPlansApi plansApi = new PricingPlansApi(pricingClient);
      PricingPlan plan = plansApi.getPricingPlan(plan_id);

      // =============================================================
      // 3.5. テナント情報取得 → plan_histories 確認
      // - 該当期間に適用されていた tax_rate を特定する
      // =============================================================
      TenantApi tenantApi = new TenantApi(authClient);
      TenantDetail tenant = tenantApi.getTenant(tenant_id);

      List<PlanHistory> histories = new ArrayList<>(tenant.getPlanHistories());
      histories.sort(Comparator.comparingLong(PlanHistory::getPlanAppliedAt));

      // 指定された plan_id かつ、start 時点で適用中だった履歴を取得（最新の1件）
      PlanHistory matchedHistory = null;
      for (int i = histories.size() - 1; i >= 0; i--) {
        PlanHistory h = histories.get(i);
        if (plan_id.equals(h.getPlanId()) && h.getPlanAppliedAt() <= start) {
          matchedHistory = h;
          break;
        }
      }

      // =============================================================
      // 3.6. 該当履歴から tax_rate_id を抽出 → 対象の税率情報を取得
      // =============================================================
      TaxRate matchedTax = null;
      if (matchedHistory != null && matchedHistory.getTaxRateId() != null) {
        TaxRateApi taxRatesApi = new TaxRateApi(pricingClient);
        List<TaxRate> allTaxRates = taxRatesApi.getTaxRates().getTaxRates();
        for (TaxRate taxRate : allTaxRates) {
          if (Objects.equals(matchedHistory.getTaxRateId(), taxRate.getId())) {
            matchedTax = taxRate;
            break;
          }
        }
      }

      // =============================================================
      // 4. メータリング使用量の取得＆金額計算
      // - planに含まれるすべてのunitに対して、該当期間内の使用量を集計
      // =============================================================
      MeteringApi meteringApi = new MeteringApi(pricingClient);
      BillingCalculationResult result = this.calcMeteringUnitBillings(
          meteringApi, plan, tenant_id, start, end);

      List<Map<String, Object>> billings = result.getBillings();
      List<Map<String, Object>> totals = result.getTotals();

      // =============================================================
      // 5. summary と pricing plan 情報の整形
      // =============================================================
      Map<String, Object> summary = new HashMap<>();
      summary.put("total_by_currency", totals);
      summary.put("total_metering_units", billings.size());

      Map<String, Object> pricingPlanInfo = new HashMap<>();
      pricingPlanInfo.put("plan_id", plan_id);
      pricingPlanInfo.put("display_name", plan.getDisplayName());
      pricingPlanInfo.put("description", plan.getDescription());

      // =============================================================
      // 6. 最終レスポンス生成
      // - メータリング情報、料金プラン情報、税率をすべて含める
      // =============================================================
      Map<String, Object> response = new HashMap<>();
      response.put("summary", summary);
      response.put("metering_unit_billings", billings);
      response.put("pricing_plan_info", pricingPlanInfo);
      response.put("tax_rate", matchedTax);

      return ResponseEntity.ok(response);

    } catch (ApiException e) {
      Map<String, String> err = new HashMap<>();
      err.put("error", "Auth API error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);

    } catch (saasus.sdk.pricing.ApiException e) {
      Map<String, String> err = new HashMap<>();
      err.put("error", "Pricing API error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);

    } catch (Exception e) {
      e.printStackTrace();
      Map<String, String> err = new HashMap<>();
      err.put("error", "Dashboard fetch failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
  }

  @GetMapping(value = "/billing/plan_periods", produces = "application/json")
  public ResponseEntity<?> getPlanPeriods(HttpServletRequest request, @RequestParam("tenant_id") String tenantId) {
    try {
      // 0. 入力チェック
      if (tenantId == null || tenantId.trim().isEmpty()) {
        Map<String, String> error = new HashMap<>();
        error.put("detail", "tenant_id required");
        return ResponseEntity.badRequest().body(error);
      }

      // 1. APIクライアント初期化
      AuthApiClient authClient = new Configuration().getAuthApiClient();
      authClient.setReferer(request.getHeader("X-Saasus-Referer"));
      PricingApiClient pricingClient = new Configuration().getPricingApiClient();
      pricingClient.setReferer(request.getHeader("X-Saasus-Referer"));

      // 2. 認証と権限チェック
      UserInfoApi userInfoApi = new UserInfoApi(authClient);
      UserInfo userInfo = userInfoApi.getUserInfo(getIDToken(request));

      if (!hasBillingAccess(userInfo, tenantId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Collections.singletonMap("error", "Insufficient permissions"));
      }

      // 3. テナント情報取得とタイムゾーン指定
      TenantApi tenantApi = new TenantApi(authClient);
      TenantDetail tenant = tenantApi.getTenant(tenantId);

      ZoneId zone = ZoneId.of("Asia/Tokyo");

      // 4. 境界点（plan history）を昇順に整列
      List<PlanHistory> sortedHistories = new ArrayList<>(tenant.getPlanHistories());
      sortedHistories.sort(Comparator.comparingLong(PlanHistory::getPlanAppliedAt));

      List<Map<String, Object>> results = new ArrayList<>();
      PricingPlansApi plansApi = new PricingPlansApi(pricingClient);

      // 5. プラン期間終了点を決定
      Integer currentEnd = tenant.getCurrentPlanPeriodEnd();
      long fixedLastEpoch = (currentEnd != null)
          ? currentEnd - 1
          : Instant.now().getEpochSecond();

      // 6. 境界ごとに分割して区間リストを作成
      for (int i = 0; i < sortedHistories.size(); i++) {
        PlanHistory hist = sortedHistories.get(i);
        String planId = hist.getPlanId();
        if (planId == null || planId.isEmpty()) {
          continue;
        }

        // プラン取得（取得できなければスキップ）
        PricingPlan plan = plansApi.getPricingPlan(planId);

        // 開始・終了日時
        long startEpoch = hist.getPlanAppliedAt();
        long endEpoch = (i + 1 < sortedHistories.size())
            ? sortedHistories.get(i + 1).getPlanAppliedAt() - 1
            : fixedLastEpoch;

        LocalDateTime periodStart = Instant.ofEpochSecond(startEpoch).atZone(zone).toLocalDateTime();
        LocalDateTime periodEnd = Instant.ofEpochSecond(endEpoch).atZone(zone).toLocalDateTime();

        // 年払いプランかどうか判定
        boolean isYearly = this.planHasYearUnit(plan);

        // 区間を step（1か月/1年）単位で分割
        LocalDateTime cur = periodStart;
        while (!cur.isAfter(periodEnd)) {
          LocalDateTime next = isYearly ? cur.plusYears(1) : cur.plusMonths(1);
          LocalDateTime segEnd = next.minusSeconds(1);
          if (segEnd.isAfter(periodEnd))
            segEnd = periodEnd;
          if (!segEnd.isAfter(cur))
            break;

          Map<String, Object> segment = new HashMap<>();
          segment.put("label", String.format("%s ～ %s",
              cur.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")),
              segEnd.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"))));
          segment.put("plan_id", planId);
          segment.put("start", cur.atZone(zone).toEpochSecond());
          segment.put("end", segEnd.atZone(zone).toEpochSecond());
          results.add(segment);

          if (segEnd.equals(periodEnd))
            break;
          cur = segEnd.plusSeconds(1);
        }
      }

      // 7. start DESC でソート
      results.sort(new Comparator<Map<String, Object>>() {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
          Long s1 = ((Number) o1.get("start")).longValue();
          Long s2 = ((Number) o2.get("start")).longValue();
          return s2.compareTo(s1); // 降順
        }
      });

      return ResponseEntity.ok(results);

    } catch (ApiException e) {
      Map<String, String> err = new HashMap<>();
      err.put("error", "Auth API error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    } catch (saasus.sdk.pricing.ApiException e) { // pricing 側
      Map<String, String> err = new HashMap<>();
      err.put("error", "Pricing API error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    } catch (Exception e) {
      e.printStackTrace();
      Map<String, String> err = new HashMap<>();
      err.put("error", "plan periods failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
  }

  private boolean planHasYearUnit(PricingPlan plan) {
    try {
      List<PricingMenu> menus = plan.getPricingMenus();
      for (PricingMenu menu : menus) {
        for (PricingUnit unit : menu.getUnits()) {
          String interval = extractRecurringInterval(unit);
          if ("year".equals(interval)) {
            return true;
          }
        }
      }
    } catch (Exception e) {
      System.err.println("[planHasYearUnit] " + e.getMessage());
    }
    return false;
  }

  private String extractRecurringInterval(PricingUnit unit) {
    Object instance = unit.getActualInstance();

    if (instance instanceof PricingFixedUnit) {
      RecurringInterval interval = ((PricingFixedUnit) instance).getRecurringInterval();
      return interval != null ? interval.getValue() : "month";
    }

    if (instance instanceof PricingUsageUnit) {
      RecurringInterval interval = ((PricingUsageUnit) instance).getRecurringInterval();
      return interval != null ? interval.getValue() : "month";
    }

    if (instance instanceof PricingTieredUsageUnit) {
      RecurringInterval interval = ((PricingTieredUsageUnit) instance).getRecurringInterval();
      return interval != null ? interval.getValue() : "month";
    }

    if (instance instanceof PricingTieredUnit) {
      RecurringInterval interval = ((PricingTieredUnit) instance).getRecurringInterval();
      return interval != null ? interval.getValue() : "month";
    }

    return "month";
  }

  @PostMapping(value = "/billing/metering/{tenantId}/{unit}/{ts}", produces = "application/json")
  public ResponseEntity<?> updateCountOfSpecifiedTimestamp(HttpServletRequest request,
      @PathVariable("tenantId") String tenantId,
      @PathVariable("unit") String unitName,
      @PathVariable("ts") long ts,
      @Valid @RequestBody UpdateMeteringCountRequest body) {
    try {
      // 1. 権限チェック
      AuthApiClient authClient = new Configuration().getAuthApiClient();
      authClient.setReferer(request.getHeader("X-Saasus-Referer"));

      UserInfoApi userInfoApi = new UserInfoApi(authClient);
      UserInfo userInfo = userInfoApi.getUserInfo(getIDToken(request));

      if (!hasBillingAccess(userInfo, tenantId)) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Insufficient permissions");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
      }

      // 2. 入力バリデーション
      String method = body.getMethod();
      int count = body.getCount();

      List<String> allowedMethods = Arrays.asList("add", "sub", "direct");
      if (!allowedMethods.contains(method)) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid method: must be one of add, sub, direct");
        return ResponseEntity.badRequest().body(error);
      }
      if (count < 0) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Count must be >= 0");
        return ResponseEntity.badRequest().body(error);
      }

      // 3. SaaSus API 呼び出し
      PricingApiClient pricingClient = new Configuration().getPricingApiClient();
      pricingClient.setReferer(request.getHeader("X-Saasus-Referer"));

      MeteringApi meteringApi = new MeteringApi(pricingClient);
      UpdateMeteringUnitTimestampCountParam param = new UpdateMeteringUnitTimestampCountParam()
          .method(UpdateMeteringUnitTimestampCountMethod.fromValue(method))
          .count(count);

      MeteringUnitTimestampCount meteringCount = meteringApi.updateMeteringUnitTimestampCount(tenantId, unitName,
          (int) ts, param);

      return ResponseEntity.ok(meteringCount);

    } catch (ApiException e) { // auth 側
      Map<String, String> err = new HashMap<>();
      err.put("error", "Auth API error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);

    } catch (saasus.sdk.pricing.ApiException e) { // pricing 側
      Map<String, String> err = new HashMap<>();
      err.put("error", "Pricing API error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);

    } catch (Exception e) { // その他
      e.printStackTrace();
      Map<String, String> err = new HashMap<>();
      err.put("error", "Failed to update metering count: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
  }

  @PostMapping(value = "/billing/metering/{tenantId}/{unit}", produces = "application/json")
  public ResponseEntity<?> updateCountOfNow(HttpServletRequest request,
      @PathVariable("tenantId") String tenantId,
      @PathVariable("unit") String unitName,
      @Valid @RequestBody UpdateMeteringCountRequest body) {
    try {
      // 1. 権限チェック
      AuthApiClient authClient = new Configuration().getAuthApiClient();
      authClient.setReferer(request.getHeader("X-Saasus-Referer"));

      UserInfoApi userInfoApi = new UserInfoApi(authClient);
      UserInfo userInfo = userInfoApi.getUserInfo(getIDToken(request));

      if (!hasBillingAccess(userInfo, tenantId)) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Insufficient permissions");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
      }

      // 2. 入力バリデーション
      String method = body.getMethod();
      int count = body.getCount();

      List<String> allowedMethods = Arrays.asList("add", "sub", "direct");
      if (!allowedMethods.contains(method)) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid method: must be one of add, sub, direct");
        return ResponseEntity.badRequest().body(error);
      }
      if (count < 0) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Count must be >= 0");
        return ResponseEntity.badRequest().body(error);
      }

      // 3. SaaSus API 呼び出し
      PricingApiClient pricingClient = new Configuration().getPricingApiClient();
      pricingClient.setReferer(request.getHeader("X-Saasus-Referer"));

      MeteringApi meteringApi = new MeteringApi(pricingClient);
      UpdateMeteringUnitTimestampCountNowParam param = new UpdateMeteringUnitTimestampCountNowParam()
          .method(UpdateMeteringUnitTimestampCountMethod.fromValue(method))
          .count(count);

      MeteringUnitTimestampCount meteringCount = meteringApi.updateMeteringUnitTimestampCountNow(tenantId, unitName,
          param);

      return ResponseEntity.ok(meteringCount);

    } catch (ApiException e) { // auth 側
      Map<String, String> err = new HashMap<>();
      err.put("error", "Auth API error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);

    } catch (saasus.sdk.pricing.ApiException e) { // pricing 側
      Map<String, String> err = new HashMap<>();
      err.put("error", "Pricing API error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);

    } catch (Exception e) { // その他
      e.printStackTrace();
      Map<String, String> err = new HashMap<>();
      err.put("error", "Failed to update metering count: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
  }
}
