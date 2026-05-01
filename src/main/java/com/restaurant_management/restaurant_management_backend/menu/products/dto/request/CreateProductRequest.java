package com.restaurant_management.restaurant_management_backend.menu.products.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(

  @NotBlank(message = "El nombre del producto es obligatorio")
  String name,

  @DecimalMin(value = "0.0", inclusive = false, message = "El precio no puede ser negativo")
  @NotNull(message = "El precio es obligatorio")
  BigDecimal price,

  @NotNull(message = "La categoria es obligatorio")
  Long categoryId

) {}
