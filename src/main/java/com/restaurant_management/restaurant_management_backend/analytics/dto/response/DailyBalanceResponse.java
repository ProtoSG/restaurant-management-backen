package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyBalanceResponse(
  LocalDate startDate,
  LocalDate endDate,
  BigDecimal totalBalance,
  Double changePercentage,
  List<DailyBalanceItemResponse> items
) {}
