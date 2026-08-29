package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * One item the mesero confirmed — every field re-validated server-side by
 * {@code VoiceOrderConfirmService} before anything is written. Mirrors
 * {@code AddOrderItemRequest}'s shape; {@code selectedPrice} must be a real, resolvable price at
 * confirm time, not just at preview time.
 */
public record VoiceOrderConfirmItem(

  @NotNull(message = "El producto es obligatorio")
  Long productId,

  @NotNull(message = "El precio es obligatorio")
  BigDecimal selectedPrice,

  @Min(value = 1, message = "La cantidad debe ser mayor a 0")
  @NotNull(message = "La cantidad es obligatoria")
  Integer quantity,

  String notes,

  boolean isTakeaway

) {}
