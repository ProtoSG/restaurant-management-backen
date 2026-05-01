package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;

public record HourlyBalanceResponse(
  String hour,
  String time,
  BigDecimal sales,
  BigDecimal cumulativeBalance,
  Integer orderCount,
  Double conversionRate
) {}
