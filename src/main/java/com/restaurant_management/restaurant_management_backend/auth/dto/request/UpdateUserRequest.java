package com.restaurant_management.restaurant_management_backend.auth.dto.request;

import com.restaurant_management.restaurant_management_backend.shared.enums.RoleName;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(

  @NotBlank(message = "El nombre es obligatorio")
  String name,

  @NotNull(message = "El rol es obligatorio")
  RoleName role

) {}
