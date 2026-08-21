package com.restaurant_management.restaurant_management_backend.menu.products.productvariants.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProductVariantRequest(
  @NotBlank(message = "El nombre de la variante es obligatorio")
  String name,

  @DecimalMin(value = "0.0", inclusive = false, message = "El precio no puede ser negativo")
  @NotNull(message = "El precio es obligatorio")
  BigDecimal price,

  @Min(value = 0, message = "El orden no puede ser negativo")
  Integer sortOrder
) {}
