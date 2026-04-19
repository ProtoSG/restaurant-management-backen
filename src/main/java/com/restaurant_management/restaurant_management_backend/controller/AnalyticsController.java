package com.restaurant_management.restaurant_management_backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.dto.BalanceIntradayDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailyBalanceDTO;
import com.restaurant_management.restaurant_management_backend.dto.CategoryProductsDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailySalesByPaymentDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailySummaryDTO;
import com.restaurant_management.restaurant_management_backend.dto.DashboardOverviewDTO;
import com.restaurant_management.restaurant_management_backend.dto.EarningsSummaryDTO;
import com.restaurant_management.restaurant_management_backend.dto.RecentPaidOrdersResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.TableTransfersResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.TopProductsResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.TopSellingProductDTO;
import com.restaurant_management.restaurant_management_backend.dto.WeeklySalesDTO;
import com.restaurant_management.restaurant_management_backend.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  @GetMapping("/daily-summary")
  public ResponseEntity<DailySummaryDTO> getDailySummary(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();

    DailySummaryDTO summaryDTO = analyticsService.getDailySummary(queryDate);

    return ResponseEntity.ok(summaryDTO);
  }

  @GetMapping("/products/top-selling")
  public ResponseEntity<List<TopSellingProductDTO>> getTopSellingProducts(
    @RequestParam(defaultValue = "10") int limit
  ) {
    if (limit < 1 || limit > 100) {
      limit = 10;
    }

    List<TopSellingProductDTO> products = analyticsService.getTopSellingProducts(limit);
    return ResponseEntity.ok(products);
  }

  @GetMapping("/dashboard-overview")
  public ResponseEntity<DashboardOverviewDTO> getDashboardOverview(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    DashboardOverviewDTO overview = analyticsService.getDashboardOverview(queryDate);
    return ResponseEntity.ok(overview);
  }

  @GetMapping("/balance-intraday")
  public ResponseEntity<BalanceIntradayDTO> getBalanceIntraday(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    BalanceIntradayDTO balance = analyticsService.getBalanceIntraday(queryDate);
    return ResponseEntity.ok(balance);
  }

  @GetMapping("/earnings-summary")
  public ResponseEntity<EarningsSummaryDTO> getEarningsSummary(
    @RequestParam(defaultValue = "daily") String period,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    
    // Validate period parameter
    if (!period.equals("daily") && !period.equals("weekly") && !period.equals("monthly")) {
      period = "daily";
    }
    
    EarningsSummaryDTO earnings = analyticsService.getEarningsSummary(period, queryDate);
    return ResponseEntity.ok(earnings);
  }

  @GetMapping("/products/top-with-period")
  public ResponseEntity<TopProductsResponseDTO> getTopProductsWithPeriod(
    @RequestParam(defaultValue = "5") int limit,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
  ) {
    if (limit < 1 || limit > 100) {
      limit = 5;
    }
    
    LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
    LocalDate end = (endDate != null) ? endDate : LocalDate.now();
    
    TopProductsResponseDTO products = analyticsService.getTopProductsWithPeriod(limit, start, end);
    return ResponseEntity.ok(products);
  }

  @GetMapping("/table-transfers")
  public ResponseEntity<TableTransfersResponseDTO> getTableTransfers(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date,
    @RequestParam(defaultValue = "10") int limit
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    
    if (limit < 1 || limit > 100) {
      limit = 10;
    }
    
    TableTransfersResponseDTO transfers = analyticsService.getTableTransfers(queryDate, limit);
    return ResponseEntity.ok(transfers);
  }

  @GetMapping("/daily-sales-by-payment")
  public ResponseEntity<DailySalesByPaymentDTO> getDailySalesByPayment(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();
    DailySalesByPaymentDTO sales = analyticsService.getDailySalesByPayment(queryDate);
    return ResponseEntity.ok(sales);
  }

  @GetMapping("/weekly-sales")
  public ResponseEntity<WeeklySalesDTO> getWeeklySales(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
  ) {
    LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(6);
    LocalDate end = (endDate != null) ? endDate : LocalDate.now();
    
    WeeklySalesDTO weeklySales = analyticsService.getWeeklySales(start, end);
    return ResponseEntity.ok(weeklySales);
  }

  @GetMapping("/balance-daily")
  public ResponseEntity<DailyBalanceDTO> getDailyBalance(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
  ) {
    LocalDate end = (endDate != null) ? endDate : LocalDate.now();
    LocalDate start = (startDate != null) ? startDate : end.minusDays(29);

    DailyBalanceDTO balance = analyticsService.getDailyBalance(start, end);
    return ResponseEntity.ok(balance);
  }

  @GetMapping("/recent-paid-orders")
  public ResponseEntity<RecentPaidOrdersResponseDTO> getRecentPaidOrders(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date,
    @RequestParam(defaultValue = "10") int limit
  ) {
    LocalDate queryDate = (date != null) ? date : LocalDate.now();

    if (limit < 1 || limit > 100) {
      limit = 10;
    }

    RecentPaidOrdersResponseDTO response = analyticsService.getRecentPaidOrders(queryDate, limit);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/products/top-by-category")
  public ResponseEntity<CategoryProductsDTO> getTopProductsByCategory(
    @RequestParam Long categoryId,
    @RequestParam(defaultValue = "5") int limit,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
  ) {
    if (limit < 1 || limit > 100) {
      limit = 5;
    }
    
    LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
    LocalDate end = (endDate != null) ? endDate : LocalDate.now();
    
    CategoryProductsDTO products = analyticsService.getTopProductsByCategory(categoryId, limit, start, end);
    return ResponseEntity.ok(products);
  }
  
}
