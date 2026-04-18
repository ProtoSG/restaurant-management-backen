package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class DailySummaryDTO {

  private LocalDate date;
  private BigDecimal totalRevenue;
  private Integer totalOrders;
  private BigDecimal averageTicket;
  private List<OrderStatusCountDTO> ordersByStatus;

}
