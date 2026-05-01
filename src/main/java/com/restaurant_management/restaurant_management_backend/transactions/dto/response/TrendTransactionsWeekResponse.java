package com.restaurant_management.restaurant_management_backend.transactions.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendTransactionsWeekResponse(
  LocalDate transactionDate,
  BigDecimal totalSum
) {}
