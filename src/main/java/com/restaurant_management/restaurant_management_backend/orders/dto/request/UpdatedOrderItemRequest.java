package com.restaurant_management.restaurant_management_backend.orders.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatedOrderItemRequest(

  @Min(value = 1, message = "La cantidad debe ser mayor a 0")
  @NotNull(message = "La cantidad es obligatoria")
  Integer quantity,
  String notes,
  Boolean isTakeaway

) {}
