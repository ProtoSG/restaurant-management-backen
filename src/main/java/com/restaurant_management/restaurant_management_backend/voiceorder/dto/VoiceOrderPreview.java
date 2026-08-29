package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.util.List;

/**
 * The deterministic, validated preview a mesero must confirm before a real order is created.
 * {@code allResolved} gates confirmation on BOTH every item resolving AND the table resolving —
 * a mesero can't confirm an order for a table that was never understood.
 */
public record VoiceOrderPreview(

  Integer tableNumber,       // as dictated, even if invalid — echoed for the mesero to see
  Long tableId,               // resolved real table id, null unless tableStatus is RESOLVED
  VoiceOrderTableStatus tableStatus,
  List<VoiceOrderPreviewItem> items,
  boolean allResolved

) {}
