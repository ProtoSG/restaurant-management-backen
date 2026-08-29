package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for the experimental voice-order extraction endpoint. */
public record VoiceOrderTestRequest(

  @NotBlank
  String text

) {}
