package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * What the mesero actually confirms — the (possibly edited) preview, sent back for a full
 * server-side re-validation. Never trusted as-is, even though it likely originated from a
 * {@link VoiceOrderPreview} this same backend built moments earlier.
 *
 * <p>{@code tableNumber} is nullable, not {@code @NotNull} — required only when
 * {@code isTakeawayOrder} is false. {@link VoiceOrderConfirmService} enforces that combination;
 * bean validation alone can't express "required unless X" across two fields.
 */
public record VoiceOrderConfirmRequest(

  Integer tableNumber,

  boolean isTakeawayOrder,

  @NotEmpty(message = "El pedido debe tener al menos un ítem")
  @Valid
  List<VoiceOrderConfirmItem> items

) {}
