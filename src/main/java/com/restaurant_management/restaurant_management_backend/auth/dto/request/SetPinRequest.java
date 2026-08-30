package com.restaurant_management.restaurant_management_backend.auth.dto.request;

import jakarta.validation.constraints.Pattern;

/**
 * ADMIN-only: assigns or resets a WAITER/CASHIER's PIN. There is no self-service "set your own
 * PIN" endpoint — the same trust boundary as password reset in this app (only ADMIN writes
 * {@code UserController.create}'s password field today, too).
 */
public record SetPinRequest(

  @Pattern(regexp = "^\\d{4}$", message = "El PIN debe tener 4 dígitos")
  String pin

) {}
