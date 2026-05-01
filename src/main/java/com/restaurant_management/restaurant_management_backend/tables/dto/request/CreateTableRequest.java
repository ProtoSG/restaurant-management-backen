package com.restaurant_management.restaurant_management_backend.tables.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTableRequest(

  @NotBlank(message = "El numero de la mesa es obligatorio")
  String number

) {}
