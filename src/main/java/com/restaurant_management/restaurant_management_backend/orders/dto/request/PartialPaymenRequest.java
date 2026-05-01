package com.restaurant_management.restaurant_management_backend.orders.dto.request;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.shared.enums.PaymentMethodType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PartialPaymenRequest(
  @NotNull(message = "El monto es obligatorio")
  @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
  BigDecimal amount,

  @NotNull(message = "El método de pago es obligatorio")
  PaymentMethodType paymentMethod
) {}
