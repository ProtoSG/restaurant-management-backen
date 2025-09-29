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
public class OrderItemDTO {

  private Long id;
  private Long productId;
  private Long orderId;
  private Integer quantity;
  private BigDecimal subTotal;

}
