package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodSummaryResponse(
  LocalDate startDate,
  LocalDate endDate,
  BigDecimal totalRevenue,
  Integer totalOrders,
  BigDecimal averageTicket
) {}
