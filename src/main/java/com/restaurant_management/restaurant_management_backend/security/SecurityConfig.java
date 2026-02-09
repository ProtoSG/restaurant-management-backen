package com.restaurant_management.restaurant_management_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.restaurant_management.restaurant_management_backend.enums.RoleName;
import com.restaurant_management.restaurant_management_backend.exceptions.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final AuthenticationProvider authenticationProvider;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/auth/**", "/health").permitAll()
        .anyRequest().authenticated()
      )
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authenticationProvider(authenticationProvider)
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
      .logout(logout ->
        logout.logoutUrl("/auth/logout")
        .permitAll()
        .addLogoutHandler((request, response, authentication) -> {
          final var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
          logout(authHeader, request, response);
        })
        .logoutSuccessHandler((request, response, authentication) -> 
          SecurityContextHolder.clearContext()
        )
      );

    return http.build();
  }

  private void logout(final String authHeader, HttpServletRequest request, HttpServletResponse response) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new UnauthorizedException("token invalid");
    }
    ResponseCookie cleared = ResponseCookie.from("refresh_token", "")
      .httpOnly(true)
      .secure(false) //NOTE: cambiar en produccion(True)
      .sameSite("Lax") //NOTE: cambiar en produccion(None)
      .path("/api/auth")
      .maxAge(0)
      .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
  }
}
