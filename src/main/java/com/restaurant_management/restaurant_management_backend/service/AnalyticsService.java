package com.restaurant_management.restaurant_management_backend.service;

import java.time.LocalDate;
import java.util.List;

import com.restaurant_management.restaurant_management_backend.dto.BalanceIntradayDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailyBalanceDTO;
import com.restaurant_management.restaurant_management_backend.dto.CategoryProductsDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailySalesByPaymentDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailySummaryDTO;
import com.restaurant_management.restaurant_management_backend.dto.DashboardOverviewDTO;
import com.restaurant_management.restaurant_management_backend.dto.EarningsSummaryDTO;
import com.restaurant_management.restaurant_management_backend.dto.TableTransfersResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.TopProductsResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.TopSellingProductDTO;
import com.restaurant_management.restaurant_management_backend.dto.RecentPaidOrdersResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.WeeklySalesDTO;

public interface AnalyticsService {
  DailySummaryDTO getDailySummary(LocalDate date);
  List<TopSellingProductDTO> getTopSellingProducts(int limit);
  
  // New methods for dashboard
  DashboardOverviewDTO getDashboardOverview(LocalDate date);
  BalanceIntradayDTO getBalanceIntraday(LocalDate date);
  EarningsSummaryDTO getEarningsSummary(String period, LocalDate date);
  TopProductsResponseDTO getTopProductsWithPeriod(int limit, LocalDate startDate, LocalDate endDate);
  TableTransfersResponseDTO getTableTransfers(LocalDate date, int limit);
  DailySalesByPaymentDTO getDailySalesByPayment(LocalDate date);
  WeeklySalesDTO getWeeklySales(LocalDate startDate, LocalDate endDate);
  CategoryProductsDTO getTopProductsByCategory(Long categoryId, int limit, LocalDate startDate, LocalDate endDate);
  DailyBalanceDTO getDailyBalance(LocalDate startDate, LocalDate endDate);
  RecentPaidOrdersResponseDTO getRecentPaidOrders(LocalDate date, int limit);
}
