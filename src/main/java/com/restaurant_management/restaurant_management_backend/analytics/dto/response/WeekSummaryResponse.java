package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record WeekSummaryResponse(
  BigDecimal totalSales,
  BigDecimal averageDailySales,
  Integer totalOrders,
  List<DailyBreakdownResponse> dailyBreakdown
) {}
