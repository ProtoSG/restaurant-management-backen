package com.restaurant_management.restaurant_management_backend.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class WeeklySalesDTO {

  private LocalDate startDate;
  private LocalDate endDate;
  private WeekSummaryDTO currentWeek;
  private WeekSummaryDTO previousWeek;
  private WeekComparisonDTO comparison;

}
