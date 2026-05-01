package com.restaurant_management.restaurant_management_backend.shared.exceptions;

public class ResourceConflictException extends RuntimeException {

  public ResourceConflictException(String message) {
    super(message);
  }
  
}
