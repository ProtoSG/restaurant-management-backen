package com.restaurant_management.restaurant_management_backend.dto;

import com.restaurant_management.restaurant_management_backend.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class OrderStatusCountDTO {

  private OrderStatus status;
  private Long count;

}
