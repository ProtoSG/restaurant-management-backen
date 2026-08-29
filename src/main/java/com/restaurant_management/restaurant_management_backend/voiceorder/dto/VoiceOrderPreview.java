package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import java.util.List;

/** The deterministic, validated preview a mesero must confirm before a real order is created. */
public record VoiceOrderPreview(

  Integer tableNumber,
  List<VoiceOrderPreviewItem> items,
  boolean allResolved

) {}
