package com.restaurant_management.restaurant_management_backend.orders.dto.response;

import com.restaurant_management.restaurant_management_backend.shared.enums.OrderStatus;

public record OrderStatusCountResponse(
  OrderStatus status,
  Long count
) {}
