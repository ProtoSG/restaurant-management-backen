package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.restaurant_management.restaurant_management_backend.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.enums.OrderType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class OrderWithOrderItemsDTO {

  private Long id;
  private String orderCode;
  private OrderStatus status;
  private OrderType type;
  private BigDecimal total;

  private List<OrderItemDTO> items;
}
