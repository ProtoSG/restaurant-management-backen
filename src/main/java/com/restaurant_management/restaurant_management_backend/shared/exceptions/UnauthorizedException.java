package com.restaurant_management.restaurant_management_backend.shared.exceptions;

public class UnauthorizedException extends RuntimeException {

  public UnauthorizedException(String message) {
    super(message);
  }
}
