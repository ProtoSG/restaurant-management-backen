package com.restaurant_management.restaurant_management_backend.orders.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;

public record OrderResponse(
  Long        orderId,
  String      orderCode,
  Long        tableId,
  String      tableNumber,
  OrderType   type,
  String      customerName,
  OrderStatus status,
  BigDecimal  total,
  BigDecimal  paidAmount,
  BigDecimal  remainingAmount,

  List<OrderItemResponse> items
) {}
