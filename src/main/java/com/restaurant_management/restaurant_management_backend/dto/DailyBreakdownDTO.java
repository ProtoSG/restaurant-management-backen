package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;
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
public class DailyBreakdownDTO {

  private LocalDate date;
  private String dayOfWeek;
  private BigDecimal sales;
  private Integer transactions;
  private Double percentageChange;

}
