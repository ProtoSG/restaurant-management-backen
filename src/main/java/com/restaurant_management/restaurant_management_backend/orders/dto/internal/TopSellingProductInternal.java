package com.restaurant_management.restaurant_management_backend.orders.dto.internal;

import java.math.BigDecimal;

public record TopSellingProductInternal(

  Long productId,
  String productName,
  String categoryName,
  Long quantitySold,
  BigDecimal totalRevenue

) {}
