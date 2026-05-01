package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;

public record PaymentMethodBreakdownResponse(
  PaymentMethodType method,
  BigDecimal amount,
  Integer transactionCount,
  Double percentage
) {}
