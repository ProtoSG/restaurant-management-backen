package com.restaurant_management.restaurant_management_backend.auth.dto.response;

public record MeResponse(
  Long id,
  String name,
  String username,
  String role
) {}
