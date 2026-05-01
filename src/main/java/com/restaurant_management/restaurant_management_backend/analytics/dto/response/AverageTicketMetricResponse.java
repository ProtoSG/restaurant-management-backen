package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AverageTicketMetricResponse(
  BigDecimal amount,
  Integer orderCount,
  Double percentageChange,
  String trend,
  List<BigDecimal> sparklineData
) {}
