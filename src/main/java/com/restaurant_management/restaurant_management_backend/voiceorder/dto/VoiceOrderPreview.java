package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.util.List;

/**
 * The deterministic, validated preview a mesero must confirm before a real order is created.
 * {@code allResolved} gates confirmation on BOTH every item resolving AND the table dimension
 * resolving — either a real table ({@code tableStatus == RESOLVED}) or a takeaway order that
 * legitimately has none ({@code isTakeawayOrder == true}, {@code tableStatus == NOT_APPLICABLE}).
 */
public record VoiceOrderPreview(

  Integer tableNumber,       // as dictated, even if invalid — echoed for the mesero to see. Null when isTakeawayOrder.
  Long tableId,               // resolved real table id, null unless tableStatus is RESOLVED
  VoiceOrderTableStatus tableStatus,
  boolean isTakeawayOrder,
  List<VoiceOrderPreviewItem> items,
  boolean allResolved

) {}
