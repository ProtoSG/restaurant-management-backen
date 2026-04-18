package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class HourlyBalanceDTO {

  private String hour;
  private String time;
  private BigDecimal sales;
  private BigDecimal cumulativeBalance;
  private Integer orderCount;
  private Double conversionRate;

}
