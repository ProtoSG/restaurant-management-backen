package com.restaurant_management.restaurant_management_backend.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.repository.UserRepository;
import com.restaurant_management.restaurant_management_backend.service.JwtService;

import io.jsonwebtoken.JwtException;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    if (request.getServletPath().contains("/auth")) {
      filterChain.doFilter(request, response);
      return;
    }

    final String jwtToken = extractTokenFromCookie(request, "access_token");
    if (jwtToken == null || jwtToken.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      final String email = jwtService.extractUsername(jwtToken);
      if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
        filterChain.doFilter(request, response);
        return;
      }

      final UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
      final Optional<User> user = userRepository.findByUsername(userDetails.getUsername());
      if (user.isEmpty()) {
        filterChain.doFilter(request, response);
        return;
      }

      final boolean isTokenValid = jwtService.isTokenValid(jwtToken, user.get());
      if (!isTokenValid) {
        filterChain.doFilter(request, response);
        return;
      }

      final var authToken = new UsernamePasswordAuthenticationToken(
        user.get(),
        null,
        userDetails.getAuthorities()
      );
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authToken);
    } catch (JwtException e) {
      // Token is expired or invalid — continue without setting authentication.
      // Spring Security will return 401 for protected endpoints.
    }

    filterChain.doFilter(request, response);
  }

  private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
    final Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    return Arrays.stream(cookies)
      .filter(cookie -> cookieName.equals(cookie.getName()))
      .map(Cookie::getValue)
      .findFirst()
      .orElse(null);
  }
}
