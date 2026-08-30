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
        // PIN login — public by design: the name picker (pin-login-users) carries no secret,
        // and pin-login itself has to be reachable before any session exists, same as /login.
        // RateLimitFilter throttles it by IP; AuthServiceImpl adds a per-account lockout on
        // top, since a 4-digit PIN is far weaker than a password.
        .requestMatchers("/auth/pin-login", "/auth/pin-login-users").permitAll()
        // API docs — public in dev
        .requestMatchers("/v3/api-docs/**", "/docs/**").permitAll()
        // Payments — ADMIN or CASHIER only
        .requestMatchers(HttpMethod.POST, "/orders/*/pay/**", "/orders/*/pay-partial").hasAnyRole("ADMIN", "CASHIER")
        // Mark as ready — ADMIN, CASHIER or WAITER (CHEF has no app access; cashier/waiter
        // mark it manually when kitchen signals physically that the order is done)
        .requestMatchers(HttpMethod.POST, "/orders/*/ready").hasAnyRole("ADMIN", "CASHIER", "WAITER")
        // Finalize order (release table after client left) — same roles as mark as ready
        .requestMatchers(HttpMethod.POST, "/orders/*/finalize").hasAnyRole("ADMIN", "CASHIER", "WAITER")
        // Delete order — deleting a full order (and cascading its payment transactions
        // via orphanRemoval) is an administrative action, not waiter/cashier-level
        .requestMatchers(HttpMethod.DELETE, "/orders/*").hasRole("ADMIN")
        // Analytics — ADMIN only
        .requestMatchers("/analytics/**").hasRole("ADMIN")
        // Quick notes — readable by any authenticated staff (waiters take orders)
        .requestMatchers(HttpMethod.GET, "/config/quick-notes").authenticated()
        // System config — ADMIN only
        .requestMatchers("/config/**").hasRole("ADMIN")
        // User management — ADMIN only
        .requestMatchers("/users/**").hasRole("ADMIN")
        // Release a stuck table (force-finalizes any lingering PAID orders and frees
        // the table) — ADMIN only, same rationale as the DELETE order guard above
        .requestMatchers(HttpMethod.POST, "/tables/*/release").hasRole("ADMIN")
        // Table write operations — ADMIN only
        .requestMatchers(HttpMethod.POST, "/tables/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.PUT, "/tables/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/tables/**").hasRole("ADMIN")
        // Menu write operations — ADMIN only
        .requestMatchers(HttpMethod.POST, "/categories/**", "/products/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.PUT, "/categories/**", "/products/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.PATCH, "/products/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/categories/**", "/products/**").hasRole("ADMIN")
        // Register new users — ADMIN only
        .requestMatchers(HttpMethod.POST, "/auth/register").hasRole("ADMIN")
        // Voice-order extraction — experimental, unvalidated path (LLM extraction + no writes),
        // ADMIN only until it's proven safe for WAITER/CASHIER use
        .requestMatchers("/voice-order-test/**").hasRole("ADMIN")
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
