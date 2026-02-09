package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.enums.PaymentMethodType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class TransactionMountGroupByPaymentMethodDTO {

  private PaymentMethodType paymentMethodType;
  private Integer transactionCount;
  private BigDecimal totalSum;

}
