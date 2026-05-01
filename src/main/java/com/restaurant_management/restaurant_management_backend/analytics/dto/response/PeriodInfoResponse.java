package com.restaurant_management.restaurant_management_backend.analytics.dto.response;

import java.time.LocalDate;

public record PeriodInfoResponse(
  LocalDate startDate,
  LocalDate endDate
) {}
