package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailySalesByPaymentResponse(
  LocalDate date,
  BigDecimal totalSales,
  Integer totalTransactions,
  List<PaymentMethodBreakdownResponse> paymentMethods
) {}
