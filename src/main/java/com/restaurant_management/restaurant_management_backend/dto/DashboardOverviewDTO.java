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
public class DashboardOverviewDTO {

  private LocalDate date;
  private DailySalesMetricDTO dailySales;
  private OccupiedTablesMetricDTO occupiedTables;
  private ProfitsMetricDTO profits;
  private AverageTicketMetricDTO averageTicket;

}
