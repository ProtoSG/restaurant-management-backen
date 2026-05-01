package com.restaurant_management.restaurant_management_backend.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.response.AuthResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Value("${cookie.secure:false}")
  private boolean cookieSecure;

  @Value("${cookie.same-site:Lax}")
  private String cookieSameSite;

  @Value("${application.security.jwt.expiration}")
  private long accessTokenExpiration;

  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshTokenExpiration;

  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String ACCESS_TOKEN = "access_token";

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
    @RequestBody @Valid LoginRequest req
  ) {
    AuthResult result = authService.login(req);

    ResponseCookie accessToken = getAccessToken(result.token());
    ResponseCookie refreshCookie = getRefreshToken(result.refreshToken());

    return ResponseEntity.status(HttpStatus.OK)
      .header(HttpHeaders.SET_COOKIE, accessToken.toString())
      .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
      .body(new AuthResponse(result.username(), result.role()));
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(
      @RequestBody @Valid RegisterRequest req
  ) {
    AuthResult result = authService.register(req);

    ResponseCookie accessToken   = getAccessToken(result.token());
    ResponseCookie refreshCookie = getRefreshToken(result.refreshToken());

    return ResponseEntity.status(HttpStatus.CREATED)
      .header(HttpHeaders.SET_COOKIE, accessToken.toString())
      .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
      .body(new AuthResponse(result.username(), result.role()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue(value = REFRESH_TOKEN) String refreshToken
  ) {
    AuthResult result = authService.refreshToken(refreshToken);

    ResponseCookie accessToken   = getAccessToken(result.token());
    ResponseCookie refreshCookie = getRefreshToken(result.refreshToken());

    return ResponseEntity.status(HttpStatus.OK)
      .header(HttpHeaders.SET_COOKIE, accessToken.toString())
      .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
      .body(new AuthResponse(result.username(), result.role()));
  }

  private ResponseCookie getRefreshToken(String token) {
    return generateCookie(
      REFRESH_TOKEN,
      token,
      "/api/auth/refresh", 
      refreshTokenExpiration
    );
    }

  private ResponseCookie getAccessToken(String token) {
    return generateCookie(
      ACCESS_TOKEN,
      token,
      "/",
      accessTokenExpiration
    );
  }

  private ResponseCookie generateCookie(
    String nameToken,
    String token,
    String path,
    Long duration
  ) {
    return ResponseCookie.from(nameToken, token)
      .httpOnly(true)
      .secure(cookieSecure)
      .sameSite(cookieSameSite)
      .path(path)
      .maxAge(Duration.ofMillis(duration))
      .build();
  }

}
