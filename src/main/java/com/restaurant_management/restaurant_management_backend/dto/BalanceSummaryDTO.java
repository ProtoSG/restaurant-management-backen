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
public class BalanceSummaryDTO {

  private Double rate;
  private Double rateChange;
  private BigDecimal balance;
  private Double balanceChange;
  private String status;

}
