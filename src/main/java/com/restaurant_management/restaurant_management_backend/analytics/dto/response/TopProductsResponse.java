package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.util.List;

public record TopProductsResponse(
  PeriodInfoResponse period,
  Integer totalProducts,
  List<ProductRankingResponse> products
) {}
