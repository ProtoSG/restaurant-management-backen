package com.restaurant_management.restaurant_management_backend.auth.dto.response;

public record UserResponse(
  Long id,
  String name,
  String username,
  String role,
  Boolean isActive
) {}
