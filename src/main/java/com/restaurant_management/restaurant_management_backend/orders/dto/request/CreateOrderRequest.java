package com.restaurant_management.restaurant_management_backend.orders.dto.request;

import com.restaurant_management.restaurant_management_backend.shared.enums.OrderType;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(

  Long tableId,

  @NotNull(message = "El tipo de orden es obligatorio")
  OrderType type,

  String customerName

) {}
