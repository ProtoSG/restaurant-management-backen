package com.restaurant_management.restaurant_management_backend.config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.exceptions.ErrorResponse;
import com.restaurant_management.restaurant_management_backend.repository.UserRepository;
import com.restaurant_management.restaurant_management_backend.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService, UserRepository userRepository) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.userRepository = userRepository;
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {

    if (request.getServletPath().contains("/auth") || request.getServletPath().contains("/health")) {
      filterChain.doFilter(request, response);
      return;
    }
    
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
        "Token de autenticación requerido", 
        "No se proporcionó token de autenticación");
      return;
    }

    try {
      final String jwtToken = authHeader.substring(7);
      final String userEmail = jwtService.extractUsername(jwtToken);
      
      if (userEmail == null) {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
          "Token inválido", 
          "No se pudo extraer información del usuario del token");
        return;
      }
      
      if (SecurityContextHolder.getContext().getAuthentication() != null) {
        filterChain.doFilter(request, response);
        return;
      }

      final UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
      final Optional<User> user = userRepository.findByEmail(userDetails.getUsername());
      
      if (user.isEmpty()) {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
          "Usuario no encontrado", 
          "El usuario asociado al token no existe");
        return;
      }

      final boolean isTokenValid = jwtService.isTokenValid(jwtToken, user.get());
      if (!isTokenValid) {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
          "Token inválido o expirado", 
          "El token proporcionado no es válido");
        return;
      }

      final var authToken = new UsernamePasswordAuthenticationToken(
        userDetails, 
        null, 
        userDetails.getAuthorities()
      );
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authToken);

      filterChain.doFilter(request, response);
      
    } catch (ExpiredJwtException e) {
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
        "Token expirado", 
        "El token de autenticación ha expirado. Por favor, inicia sesión nuevamente");
    } catch (JwtException e) {
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
        "Token inválido", 
        "El token de autenticación no es válido: " + e.getMessage());
    } catch (Exception e) {
      sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, 
        "Error de autenticación", 
        "Ocurrió un error al procesar la autenticación");
    }
  }

  private void sendErrorResponse(
      HttpServletResponse response, 
      HttpStatus status, 
      String message, 
      String error
  ) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    
    ErrorResponse errorResponse = new ErrorResponse(message, status.value(), error);
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
