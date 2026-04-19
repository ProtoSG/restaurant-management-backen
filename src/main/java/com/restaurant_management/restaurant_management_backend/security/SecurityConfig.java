package com.restaurant_management.restaurant_management_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final AuthenticationProvider authenticationProvider;

  @Value("${cookie.secure:false}")
  private boolean cookieSecure;

  @Value("${cookie.same-site:Lax}")
  private String cookieSameSite;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/auth/**", "/health", "/ws/**").permitAll()
        .anyRequest().authenticated()
      )
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authenticationProvider(authenticationProvider)
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
      .logout(logout ->
        logout.logoutUrl("/auth/logout")
        .permitAll()
        .addLogoutHandler((request, response, authentication) -> {
          logout(request, response);
        })
        .logoutSuccessHandler((request, response, authentication) ->
          SecurityContextHolder.clearContext()
        )
      );

    return http.build();
  }

  private void logout(HttpServletRequest request, HttpServletResponse response) {
    ResponseCookie clearedAccessToken = ResponseCookie.from("access_token", "")
      .httpOnly(true)
      .secure(cookieSecure)
      .sameSite(cookieSameSite)
      .path("/")
      .maxAge(0)
      .build();

    ResponseCookie clearedRefreshToken = ResponseCookie.from("refresh_token", "")
      .httpOnly(true)
      .secure(cookieSecure)
      .sameSite(cookieSameSite)
      .path("/api/auth/refresh")
      .maxAge(0)
      .build();

    response.addHeader(HttpHeaders.SET_COOKIE, clearedAccessToken.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, clearedRefreshToken.toString());
  }
}
