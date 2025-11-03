package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.restaurant_management.restaurant_management_backend.enums.PaymentMethodType;
import com.restaurant_management.restaurant_management_backend.enums.TransactionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class TransactionDTO {

  private Long id;
  private Long orderId;
  private BigDecimal total;
  private PaymentMethodType paymentMethod;
  private TransactionStatus status;
  private LocalDateTime transactionDate;

}
