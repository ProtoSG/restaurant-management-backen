package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
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
public class OrderItemDTO {

  private Long id;

  @NotNull(message = "El producto es obligatorio")
  private Long productId;

  private Long orderId;

  @Min(value = 1, message = "La cantidad debe ser mayor a 0")
  @NotNull(message = "La cantidad es obligatoria")
  private Integer quantity;


  private BigDecimal subTotal;

}
