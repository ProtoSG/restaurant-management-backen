package com.restaurant_management.restaurant_management_backend.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponse {

  private String message;
  private int statusCode;
  private String errorDetails;

}
