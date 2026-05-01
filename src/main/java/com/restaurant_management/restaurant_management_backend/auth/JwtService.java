package com.restaurant_management.restaurant_management_backend.auth;

import com.restaurant_management.restaurant_management_backend.auth.entity.User;

public interface JwtService {

  String generateToken(final User user);
  String generateRefreshToken(final User user);
  String extractUsername(final String token);
  boolean isTokenValid(final String token, final User user);

}
