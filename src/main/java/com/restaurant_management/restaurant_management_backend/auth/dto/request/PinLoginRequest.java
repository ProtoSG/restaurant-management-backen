package com.restaurant_management.restaurant_management_backend.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PinLoginRequest(

  @NotNull(message = "El usuario es obligatorio")
  Long userId,

  @Pattern(regexp = "^\\d{4}$", message = "El PIN debe tener 4 dígitos")
  String pin

) {}
