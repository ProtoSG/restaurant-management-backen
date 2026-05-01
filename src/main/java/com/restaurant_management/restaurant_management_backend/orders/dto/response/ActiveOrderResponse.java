package com.restaurant_management.restaurant_management_backend.orders.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;
import com.restaurant_management.restaurant_management_backend.transactions.dto.response.TransactionResponse;

public record ActiveOrderResponse(
  Long id,
  String orderCode,
  OrderStatus status,
  OrderType type,
  BigDecimal total,
  List<ActiveOrderItemResponse> items,
  BigDecimal paidAmount,
  BigDecimal remainingAmount,
  List<TransactionResponse> transactions
) {}
