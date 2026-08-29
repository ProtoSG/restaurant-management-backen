package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * What the mesero actually confirms — the (possibly edited) preview, sent back for a full
 * server-side re-validation. Never trusted as-is, even though it likely originated from a
 * {@link VoiceOrderPreview} this same backend built moments earlier.
 */
public record VoiceOrderConfirmRequest(

  @NotNull(message = "La mesa es obligatoria")
  Integer tableNumber,

  @NotEmpty(message = "El pedido debe tener al menos un ítem")
  @Valid
  List<VoiceOrderConfirmItem> items

) {}
