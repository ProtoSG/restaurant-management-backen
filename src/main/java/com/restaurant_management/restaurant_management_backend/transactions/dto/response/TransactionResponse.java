package com.restaurant_management.restaurant_management_backend.transactions.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.shared.enums.TransactionStatus;

public record TransactionResponse(
  Long id,
  Long orderId,
  Long userId,
  String userName,
  BigDecimal total,
  PaymentMethodType paymentMethod,
  TransactionStatus status,
  LocalDateTime transactionDate
) {}
