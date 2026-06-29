package com.restaurant_management.restaurant_management_backend.tables.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTableRequest(
  @NotBlank(message = "El número de mesa es obligatorio")
  @Size(max = 50, message = "El número de mesa no puede exceder 50 caracteres")
  String number
) {}
