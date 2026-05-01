package com.restaurant_management.restaurant_management_backend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(

  @NotBlank(message = "El username es obligatorio")
  String username,

  @NotNull(message = "La contraseña es obligatoria")
  String password

) {}
