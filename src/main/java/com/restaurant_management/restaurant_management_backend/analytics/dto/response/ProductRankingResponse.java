package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;

public record ProductRankingResponse(
  Integer rank,
  Long productId,
  String productName,
  String categoryName,
  Long quantitySold,
  BigDecimal totalRevenue,
  BigDecimal averagePrice
) {}
