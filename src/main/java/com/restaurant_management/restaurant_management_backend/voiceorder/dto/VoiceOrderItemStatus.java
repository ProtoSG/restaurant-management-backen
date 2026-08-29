package com.restaurant_management.restaurant_management_backend.voiceorder.dto;

/** Result of deterministically validating one extracted item against the real catalog. */
public enum VoiceOrderItemStatus {
  /** productId resolves to an available product, and selectedPrice matches a real price/variant. */
  RESOLVED,
  /** productId resolves to an available product, but selectedPrice matches no real price/variant. */
  PRICE_MISMATCH,
  /** productId is null, or does not resolve to any product in the catalog. */
  NOT_FOUND,
  /** productId resolves to a product, but the product (or the matched context) is not available. */
  NOT_AVAILABLE
}
