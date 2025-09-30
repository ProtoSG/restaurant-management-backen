package com.restaurant_management.restaurant_management_backend.dto;

import com.restaurant_management.restaurant_management_backend.enums.OrderType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter @Getter
public class OrderTypeDTO {

  @NotNull(message = "El tipo es obligatorio")
  private OrderType type;

}
