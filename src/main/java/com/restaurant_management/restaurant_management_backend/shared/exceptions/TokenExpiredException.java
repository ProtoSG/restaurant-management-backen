package com.restaurant_management.restaurant_management_backend.shared.exceptions;

public class TokenExpiredException extends RuntimeException {

  public TokenExpiredException(String message) {
    super(message);
  }
}
