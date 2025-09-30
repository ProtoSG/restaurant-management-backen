package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.enums.OrderStatus;
import com.restaurant_management.restaurant_management_backend.enums.OrderType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class OrderDTO {

  private Long id;

  @NotNull(message = "La mesa es obligatoria")
  private Long tableId;

  private OrderType type;

  private OrderStatus status;

  private BigDecimal total;

}
