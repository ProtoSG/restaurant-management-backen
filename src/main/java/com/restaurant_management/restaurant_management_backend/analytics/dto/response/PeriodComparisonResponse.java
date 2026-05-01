package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

public record PeriodComparisonResponse(
  Double revenueChange,
  Double ordersChange,
  Double ticketChange,
  String message
) {}
