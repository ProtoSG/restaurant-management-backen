package com.restaurant_management.restaurant_management_backend.transactions.dto.response;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;

public record TransactionMountGroupByPaymentMethodResponse(
  PaymentMethodType paymentMethodType,
  Integer transactionCount,
  BigDecimal totalSum
) {}
