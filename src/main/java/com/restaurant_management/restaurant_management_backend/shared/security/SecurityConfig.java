package com.restaurant_management.restaurant_management_backend.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.restaurant_management.restaurant_management_backend.auth.CookieService;
import com.restaurant_management.restaurant_management_backend.auth.RefreshTokenRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final RateLimitFilter rateLimitFilter;
  private final AuthenticationProvider authenticationProvider;
  private final CookieService cookieService;
  private final RefreshTokenRepository refreshTokenRepository;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        // Health check — public
        .requestMatchers("/health").permitAll()
        // Auth endpoints — public
        .requestMatchers("/auth/login", "/auth/refresh").permitAll()
        // API docs — public in dev
        .requestMatchers("/v3/api-docs/**", "/docs/**").permitAll()
        // Payments — ADMIN or CASHIER only
        .requestMatchers(HttpMethod.POST, "/orders/*/pay/**", "/orders/*/pay-partial").hasAnyRole("ADMIN", "CASHIER")
        // Mark as ready — ADMIN or CHEF
        .requestMatchers(HttpMethod.POST, "/orders/*/ready").hasAnyRole("ADMIN", "CHEF")
        // Analytics — ADMIN or CASHIER
        .requestMatchers("/analytics/**").hasAnyRole("ADMIN", "CASHIER")
        // System config — ADMIN only
        .requestMatchers("/config/**").hasRole("ADMIN")
        // User management — ADMIN only
        .requestMatchers("/users/**").hasRole("ADMIN")
        // Menu write operations — ADMIN only
        .requestMatchers(HttpMethod.POST, "/categories/**", "/products/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.PUT, "/categories/**", "/products/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.PATCH, "/products/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/categories/**", "/products/**").hasRole("ADMIN")
        // Register new users — ADMIN only
        .requestMatchers(HttpMethod.POST, "/auth/register").hasRole("ADMIN")
        // Everything else requires authentication
        .anyRequest().authenticated()
      )
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authenticationProvider(authenticationProvider)
      .addFilterBefore(rateLimitFilter, LogoutFilter.class)
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
      .logout(logout ->
        logout.logoutUrl("/auth/logout")
          .permitAll()
          .addLogoutHandler((request, response, authentication) -> {
            revokeRefreshTokenFromCookie(request.getCookies());
            clearAuthCookies(response);
          })
          .logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
      );

    return http.build();
  }

  private void revokeRefreshTokenFromCookie(Cookie[] cookies) {
    if (cookies == null) return;
    for (Cookie cookie : cookies) {
      if (CookieService.REFRESH_TOKEN.equals(cookie.getName())) {
        refreshTokenRepository.findByToken(cookie.getValue()).ifPresent(rt -> {
          rt.revoke();
          refreshTokenRepository.save(rt);
        });
        return;
      }
    }
  }

  private void clearAuthCookies(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clearAccessToken().toString());
    response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clearRefreshToken().toString());
  }
}
