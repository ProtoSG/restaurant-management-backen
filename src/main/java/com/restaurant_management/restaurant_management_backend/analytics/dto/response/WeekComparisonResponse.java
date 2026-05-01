package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

public record WeekComparisonResponse(
  Double salesChange,
  Double ordersChange
) {}
