package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecentPaidOrderResponse(
  Long transactionId,
  Long orderId,
  String orderCode,
  String tableNumber,
  String customerName,
  String orderType,
  BigDecimal total,
  String paymentMethod,
  LocalDateTime paidAt,
  String cashierName
) {}
