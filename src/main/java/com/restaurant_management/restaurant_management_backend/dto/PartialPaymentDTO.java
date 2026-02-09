package com.restaurant_management.restaurant_management_backend.dto;

import java.math.BigDecimal;

import com.restaurant_management.restaurant_management_backend.enums.PaymentMethodType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
public class PartialPaymentDTO {

  @NotNull(message = "El monto es obligatorio")
  @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
  private BigDecimal amount;

  @NotNull(message = "El método de pago es obligatorio")
  private PaymentMethodType paymentMethod;

}
