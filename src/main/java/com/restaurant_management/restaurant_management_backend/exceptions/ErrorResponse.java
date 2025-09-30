package com.restaurant_management.restaurant_management_backend.exceptions;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponse {

  private String message;
  private int statusCode;
  private LocalDateTime timestamp;
  private String errorDetails;

  public ErrorResponse(String message, int statusCode, String errorDetails) {
    this.message = message;
    this.statusCode = statusCode;
    this.timestamp = LocalDateTime.now();
    this.errorDetails = errorDetails;
  }

}
