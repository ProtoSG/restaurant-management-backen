package com.restaurant_management.restaurant_management_backend.auth.dto.internal;

public record AuthResult(
  String username,
  String role,
  String token,
  String refreshToken
) {}
