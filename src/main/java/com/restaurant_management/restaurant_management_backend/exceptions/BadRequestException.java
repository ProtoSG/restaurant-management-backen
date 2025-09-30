package com.restaurant_management.restaurant_management_backend.exceptions;

public class BadRequestException extends RuntimeException {
  
  public BadRequestException(String message) {
    super(message);
  }
  
}
