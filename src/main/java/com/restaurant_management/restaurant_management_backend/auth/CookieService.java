package com.restaurant_management.restaurant_management_backend.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieService {

  public static final String ACCESS_TOKEN = "access_token";
  public static final String REFRESH_TOKEN = "refresh_token";
  public static final String REFRESH_TOKEN_PATH = "/api/auth/refresh";

  @Value("${cookie.secure:false}")
  private boolean secure;

  @Value("${cookie.same-site:Lax}")
  private String sameSite;

  @Value("${application.security.jwt.expiration}")
  private long accessTokenExpiration;

  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshTokenExpiration;

  public ResponseCookie buildAccessToken(String token) {
    return build(ACCESS_TOKEN, token, "/", accessTokenExpiration);
  }

  public ResponseCookie buildRefreshToken(String token) {
    return build(REFRESH_TOKEN, token, REFRESH_TOKEN_PATH, refreshTokenExpiration);
  }

  public ResponseCookie clearAccessToken() {
    return build(ACCESS_TOKEN, "", "/", 0);
  }

  public ResponseCookie clearRefreshToken() {
    return build(REFRESH_TOKEN, "", REFRESH_TOKEN_PATH, 0);
  }

  private ResponseCookie build(String name, String value, String path, long maxAgeMillis) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(path)
        .maxAge(maxAgeMillis == 0 ? Duration.ZERO : Duration.ofMillis(maxAgeMillis))
        .build();
  }
}
