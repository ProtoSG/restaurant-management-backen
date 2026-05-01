package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.time.LocalDate;

public record WeeklySalesResponse(
  LocalDate startDate,
  LocalDate endDate,
  WeekSummaryResponse currentWeek,
  WeekSummaryResponse previousWeek,
  WeekComparisonResponse comparison
) {}
