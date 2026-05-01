package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyBalanceItemResponse(
  LocalDate date,
  String label,
  BigDecimal totalSales
) {}
