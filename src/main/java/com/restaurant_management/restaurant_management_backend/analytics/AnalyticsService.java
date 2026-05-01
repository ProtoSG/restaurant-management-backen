package com.restaurant_management.restaurant_management_backend.analytics;

import java.time.LocalDate;
import java.util.List;

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

public interface AnalyticsService {
  DailySummaryResponse getDailySummary(LocalDate date);
  List<TopSellingProductInternal> getTopSellingProducts(int limit);
  DashboardOverviewResponse getDashboardOverview(LocalDate date);
  BalanceIntradayResponse getBalanceIntraday(LocalDate date);
  EarningsSummaryResponse getEarningsSummary(String period, LocalDate date);
  TopProductsResponse getTopProductsWithPeriod(int limit, LocalDate startDate, LocalDate endDate);
  DailySalesByPaymentResponse getDailySalesByPayment(LocalDate date);
  WeeklySalesResponse getWeeklySales(LocalDate startDate, LocalDate endDate);
  CategoryProductsResponse getTopProductsByCategory(Long categoryId, int limit, LocalDate startDate, LocalDate endDate);
  DailyBalanceResponse getDailyBalance(LocalDate startDate, LocalDate endDate);
  RecentPaidOrdersResponse getRecentPaidOrders(LocalDate date, int limit);
}
