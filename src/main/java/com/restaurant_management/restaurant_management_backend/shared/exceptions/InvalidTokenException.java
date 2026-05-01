package com.restaurant_management.restaurant_management_backend.shared.exceptions;

public class InvalidTokenException extends RuntimeException {

  public InvalidTokenException(String message) {
    super(message);
  }
}
