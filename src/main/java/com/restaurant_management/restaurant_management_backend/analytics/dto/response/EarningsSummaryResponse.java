package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

public record EarningsSummaryResponse(
  String period,
  PeriodSummaryResponse currentPeriod,
  PeriodSummaryResponse previousPeriod,
  PeriodComparisonResponse comparison,
  Integer profitMargin,
  String note
) {}
