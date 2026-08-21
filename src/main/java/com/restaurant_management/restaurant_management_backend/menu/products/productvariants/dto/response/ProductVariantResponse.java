package com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.response;

import java.math.BigDecimal;

public record ProductVariantResponse(
  Long id,
  String name,
  BigDecimal price,
  Boolean isAvailable,
  Integer sortOrder
) {}
