package com.restaurant_management.restaurant_management_backend.auth.dto.request;

import com.restaurant_management.restaurant_management_backend.shared.enums.RoleName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(

  @NotBlank(message = "El nombre el obligatoria")
  String name,

  @NotBlank(message = "El username es obligatorio")
  String username,

  @NotBlank(message = "La contraseña es obligatoria")
  String password,

  @NotNull(message = "El rol es obligatorio")
  RoleName role

) {}
