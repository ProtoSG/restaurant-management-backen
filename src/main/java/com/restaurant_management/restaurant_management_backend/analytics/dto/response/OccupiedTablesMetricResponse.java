package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.util.List;

public record OccupiedTablesMetricResponse(
  Integer occupied,
  Integer total,
  Double percentageOccupancy,
  List<Integer> sparklineData
) {}
