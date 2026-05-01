package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CategoryProductsResponse(
  Long categoryId,
  String categoryName,
  BigDecimal totalSales,
  Long totalQuantity,
  List<ProductRankingResponse> products
) {}
