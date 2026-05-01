package com.restaurant_management.restaurant_management_backend.orders.dto.response;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ActiveProductResponse;

public record ActiveOrderItemResponse(
  Long id,
  Integer quantity,
  BigDecimal subTotal,
  ActiveProductResponse product
) {}
