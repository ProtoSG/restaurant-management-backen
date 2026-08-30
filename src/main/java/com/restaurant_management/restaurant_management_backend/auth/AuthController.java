package com.restaurant_management.restaurant_management_backend.auth;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.PinLoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.response.AuthResponse;
import com.restaurant_management.restaurant_management_backend.auth.dto.response.MeResponse;
import com.restaurant_management.restaurant_management_backend.auth.dto.response.PinLoginCandidate;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final CookieService cookieService;

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
    AuthResult result = authService.login(req);
    return authResponse(HttpStatus.OK, result);
  }

  // Public — the eligible-staff list itself carries no secret (see PinLoginCandidate javadoc);
  // it's what the tablet shows before any authentication, same trust level as the login form.
  @GetMapping("/pin-login-users")
  public ResponseEntity<List<PinLoginCandidate>> pinLoginUsers() {
    return ResponseEntity.ok(authService.listPinLoginCandidates());
  }

  @PostMapping("/pin-login")
  public ResponseEntity<AuthResponse> pinLogin(@RequestBody @Valid PinLoginRequest req) {
    AuthResult result = authService.pinLogin(req);
    return authResponse(HttpStatus.OK, result);
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest req) {
    AuthResult result = authService.register(req);
    return authResponse(HttpStatus.CREATED, result);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue(value = CookieService.REFRESH_TOKEN, required = false) String refreshToken
  ) {
    AuthResult result = authService.refreshToken(refreshToken);
    return authResponse(HttpStatus.OK, result);
  }

  @GetMapping("/me")
  public ResponseEntity<MeResponse> me(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    return ResponseEntity.ok(new MeResponse(
        user.getId(),
        user.getName(),
        user.getUsername(),
        user.getRole().getName().name()
    ));
  }

  private ResponseEntity<AuthResponse> authResponse(HttpStatus status, AuthResult result) {
    ResponseCookie access = cookieService.buildAccessToken(result.token());
    ResponseCookie refresh = cookieService.buildRefreshToken(result.refreshToken());

    return ResponseEntity.status(status)
        .header(HttpHeaders.SET_COOKIE, access.toString())
        .header(HttpHeaders.SET_COOKIE, refresh.toString())
        .body(new AuthResponse(result.username(), result.role()));
  }
}
