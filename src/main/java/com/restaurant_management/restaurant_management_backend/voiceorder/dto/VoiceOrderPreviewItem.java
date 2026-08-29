package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.math.BigDecimal;

/** One item in the editable preview shown to the mesero before any order is created. */
public record VoiceOrderPreviewItem(

  VoiceOrderItemStatus status,
  String rawText,
  Long productId,     // as extracted, even if invalid — for the mesero to see what was guessed
  String productName,  // resolved name if productId is valid, else null
  BigDecimal selectedPrice,
  Integer quantity,
  String notes

) {}
