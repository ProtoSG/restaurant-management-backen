package com.restaurant_management.restaurant_management_backend.orders.dto.response;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.menu.products.dto.response.ProductResponse;

public record OrderItemResponse(
  Long orderId,
  Integer quantity,
  BigDecimal subTotal,
  ProductResponse product,
  String notes
) {}
