package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;

public record BalanceSummaryResponse(
  Double rate,
  Double rateChange,
  BigDecimal balance,
  Double balanceChange,
  String status
) {}
