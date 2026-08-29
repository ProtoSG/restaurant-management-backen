package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

/** Result of deterministically resolving the dictated table number against the real catalog. */
public enum VoiceOrderTableStatus {
  /** A table number was dictated and matches an active real table. */
  RESOLVED,
  /** No table number was dictated at all — the mesero needs to say which mesa. */
  MISSING,
  /** A table number was dictated, but no active table with that number exists. */
  NOT_FOUND
}
