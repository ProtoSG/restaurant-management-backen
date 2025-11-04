package com.restaurant_management.restaurant_management_backend.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant_management.restaurant_management_backend.exceptions.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public CustomAuthenticationEntryPoint() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  @Override
  public void commence(
      HttpServletRequest request, 
      HttpServletResponse response,
      AuthenticationException authException
  ) throws IOException, ServletException {
    
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    
    ErrorResponse errorResponse = new ErrorResponse(
      "Acceso denegado. Se requiere autenticación válida",
      HttpStatus.UNAUTHORIZED.value(),
      "No autenticado"
    );
    
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
