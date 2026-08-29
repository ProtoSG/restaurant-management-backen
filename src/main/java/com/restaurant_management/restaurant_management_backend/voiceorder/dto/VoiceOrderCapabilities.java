package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

/**
 * Reports which input path the client should offer. The backend can't invoke native device
 * dictation itself — that's entirely client-side — but it CAN tell the client whether the
 * Whisper/audio path is actually configured, so the client picks the right UI: record audio
 * (POST /voice-order-test/audio) when Whisper is available, or dictate via the device's native
 * keyboard mic and POST the resulting text ({@code POST /voice-order-test}) when it isn't.
 */
public record VoiceOrderCapabilities(

  boolean audioTranscriptionAvailable

) {}
