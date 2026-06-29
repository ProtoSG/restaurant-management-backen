package com.restaurant_management.restaurant_management_backend.orders.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record KitchenConfirmRequest(

  @NotEmpty(message = "La lista de ítems es obligatoria")
  @Valid
  List<KitchenLineRef> items

) {}
