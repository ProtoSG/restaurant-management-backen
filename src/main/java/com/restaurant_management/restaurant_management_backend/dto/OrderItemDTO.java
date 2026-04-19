package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

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
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Long productId;

  @Min(value = 1, message = "La cantidad debe ser mayor a 0")
  @NotNull(message = "La cantidad es obligatoria")
  private Integer quantity;

  // @NotNull(message = "El precio es obligatorio")
  // @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private BigDecimal price;

  private BigDecimal subTotal;

  private ProductDTO product;

  private String notes;

}
