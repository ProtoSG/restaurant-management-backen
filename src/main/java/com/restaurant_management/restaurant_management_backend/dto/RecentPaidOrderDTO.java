package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class RecentPaidOrderDTO {

  private Long transactionId;
  private Long orderId;
  private String orderCode;
  private String tableNumber;
  private String customerName;
  private String orderType;
  private BigDecimal total;
  private String paymentMethod;
  private LocalDateTime paidAt;
  private String cashierName;

}
