package com.restaurant_management.restaurant_management_backend.orders.dto.request;

/**
 * Cuerpo opcional del endpoint POST /orders/{id}/pay/{paymentMethod}. El método de
 * pago sigue viajando como path variable; este body solo agrega la clave de
 * idempotencia para permitir reintentos seguros ante fallos de red ambiguos.
 */
public record PayOrderRequest(
  String idempotencyKey
) {}
