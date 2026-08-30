package com.restaurant_management.restaurant_management_backend.auth.dto.response;

/**
 * One entry in the public PIN-login name picker. Deliberately minimal — no username, no role
 * beyond what's needed to render an icon: the userId itself is already meant to be public (the
 * whole point of "pick your name from a list"), the PIN is the only secret in this flow.
 */
public record PinLoginCandidate(
  Long id,
  String name
) {}
