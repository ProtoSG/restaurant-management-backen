package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProfitsMetricResponse(
  BigDecimal amount,
  Double percentageChange,
  String note,
  String trend,
  List<BigDecimal> sparklineData
) {}
