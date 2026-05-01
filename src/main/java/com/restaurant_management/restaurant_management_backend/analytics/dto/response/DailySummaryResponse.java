package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderStatusCountResponse;

public record DailySummaryResponse(
  LocalDate date,
  BigDecimal totalRevenue,
  Integer totalOrders,
  BigDecimal averageTicket,
  List<OrderStatusCountResponse> ordersByStatus
) {}
