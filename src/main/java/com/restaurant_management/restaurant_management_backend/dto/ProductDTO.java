package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class ProductDTO {

  private Long id;

  @NotBlank(message = "El nombre del producto es obligatorio")
  private String name;

  @DecimalMin(value = "0.0", inclusive = false, message = "El precio no puede ser negativo")
  @NotNull(message = "El precio es obligatorio")
  private BigDecimal price;

  @NotNull(message = "La categoria es obligatorio")
  private Long categoryId;

}
