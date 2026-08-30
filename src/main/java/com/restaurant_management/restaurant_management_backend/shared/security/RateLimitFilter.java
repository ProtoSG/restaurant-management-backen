package com.restaurant_management.restaurant_management_backend.shared.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  // /auth/pin-login shares this same per-IP throttle — a 4-digit PIN needs it even more than a
  // real password does, and AuthServiceImpl's per-account lockout only covers requests that
  // name a real, PIN-eligible userId, not a flood aimed at made-up ones.
  private static final Set<String> RATE_LIMITED_PATHS = Set.of("/auth/login", "/auth/pin-login");

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  private Bucket newBucket() {
    return Bucket.builder()
        .addLimit(Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofMinutes(1))
            .build())
        .build();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    if (!RATE_LIMITED_PATHS.contains(request.getServletPath())) {
      filterChain.doFilter(request, response);
      return;
    }

    String ip = resolveClientIp(request);
    Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"message\":\"Demasiados intentos. Intenta en 1 minuto.\",\"code\":\"RATE_LIMIT_EXCEEDED\"}");
    }
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
