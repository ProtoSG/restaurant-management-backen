package com.restaurant_management.restaurant_management_backend.analytics;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.analytics.dto.response.BalanceIntradayResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.CategoryProductsResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailyBalanceResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailySalesByPaymentResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailySummaryResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DashboardOverviewResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.EarningsSummaryResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.RecentPaidOrdersResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.TopProductsResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.WeeklySalesResponse;
import com.restaurant_management.restaurant_management_backend.orders.dto.internal.TopSellingProductInternal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  @GetMapping("/daily-summary")
  public ResponseEntity<DailySummaryResponse> getDailySummary(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    return ResponseEntity.ok(analyticsService.getDailySummary(queryDate));
  }

  @GetMapping("/products/top-selling")
  public ResponseEntity<List<TopSellingProductInternal>> getTopSellingProducts(
    @RequestParam(defaultValue = "10") int limit
  ) {
    if (limit < 1 || limit > 100) limit = 10;
    return ResponseEntity.ok(analyticsService.getTopSellingProducts(limit));
  }

  @GetMapping("/dashboard-overview")
  public ResponseEntity<DashboardOverviewResponse> getDashboardOverview(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    return ResponseEntity.ok(analyticsService.getDashboardOverview(queryDate));
  }

  @GetMapping("/balance-intraday")
  public ResponseEntity<BalanceIntradayResponse> getBalanceIntraday(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    return ResponseEntity.ok(analyticsService.getBalanceIntraday(queryDate));
  }

  @GetMapping("/earnings-summary")
  public ResponseEntity<EarningsSummaryResponse> getEarningsSummary(
    @RequestParam(defaultValue = "daily") String period,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    if (!period.equals("daily") && !period.equals("weekly") && !period.equals("monthly")) {
      period = "daily";
    }
    return ResponseEntity.ok(analyticsService.getEarningsSummary(period, queryDate));
  }

  @GetMapping("/products/top-with-period")
  public ResponseEntity<TopProductsResponse> getTopProductsWithPeriod(
    @RequestParam(defaultValue = "5") int limit,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
  ) {
    if (limit < 1 || limit > 100) limit = 5;
    LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
    LocalDate end = (endDate != null) ? endDate : LocalDate.now();
    return ResponseEntity.ok(analyticsService.getTopProductsWithPeriod(limit, start, end));
  }

  @GetMapping("/daily-sales-by-payment")
  public ResponseEntity<DailySalesByPaymentResponse> getDailySalesByPayment(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    return ResponseEntity.ok(analyticsService.getDailySalesByPayment(queryDate));
  }

  @GetMapping("/weekly-sales")
  public ResponseEntity<WeeklySalesResponse> getWeeklySales(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
  ) {
    LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(6);
    LocalDate end = (endDate != null) ? endDate : LocalDate.now();
    return ResponseEntity.ok(analyticsService.getWeeklySales(start, end));
  }

  @GetMapping("/balance-daily")
  public ResponseEntity<DailyBalanceResponse> getDailyBalance(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
  ) {
    LocalDate end = (endDate != null) ? endDate : LocalDate.now();
    LocalDate start = (startDate != null) ? startDate : end.minusDays(29);
    return ResponseEntity.ok(analyticsService.getDailyBalance(start, end));
  }

  @GetMapping("/recent-paid-orders")
  public ResponseEntity<RecentPaidOrdersResponse> getRecentPaidOrders(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date,
    @RequestParam(defaultValue = "10") int limit
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    if (limit < 1 || limit > 100) limit = 10;
    return ResponseEntity.ok(analyticsService.getRecentPaidOrders(queryDate, limit));
  }

  @GetMapping("/products/top-by-category")
  public ResponseEntity<CategoryProductsResponse> getTopProductsByCategory(
    @RequestParam Long categoryId,
    @RequestParam(defaultValue = "5") int limit,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
  ) {
    if (limit < 1 || limit > 100) limit = 5;
    LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
    LocalDate end = (endDate != null) ? endDate : LocalDate.now();
    return ResponseEntity.ok(analyticsService.getTopProductsByCategory(categoryId, limit, start, end));
  }

}
