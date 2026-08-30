package com.restaurant_management.restaurant_management_backend.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.PinLoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.response.PinLoginCandidate;
import com.restaurant_management.restaurant_management_backend.auth.entity.RefreshToken;
import com.restaurant_management.restaurant_management_backend.auth.entity.Role;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.enums.RoleName;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.UnauthorizedException;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  // Roles allowed to use PIN login at all — deliberately excludes ADMIN: a 4-digit PIN is a
  // much weaker secret than a real password, and ADMIN can touch config/users/deletions.
  // CASHIER is included (handles payments) per explicit product decision; if that changes,
  // this is the one place to narrow it back to WAITER only.
  private static final List<RoleName> PIN_ELIGIBLE_ROLES = List.of(RoleName.WAITER, RoleName.CASHIER);
  private static final int MAX_PIN_ATTEMPTS = 5;
  private static final long PIN_LOCKOUT_MINUTES = 1;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshTokenExpiration;

  @Override
  @Transactional
  public AuthResult login(LoginRequest req) {

    // authenticationManager (DaoAuthenticationProvider) validates the password and
    // hides "user not found" as BadCredentials, so a single generic message here
    // prevents username enumeration via distinct errors/timing.
    try {
      authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
          req.username(),
          req.password()
        )
      );
    } catch (AuthenticationException e) {
      throw new UnauthorizedException("Usuario o contraseña incorrectos");
    }

    User user = userRepository.findByUsername(req.username())
      .orElseThrow(() -> new UnauthorizedException("Usuario o contraseña incorrectos"));

    String jwtToken      = jwtService.generateToken(user);
    String refreshTokenStr = jwtService.generateRefreshToken(user);

    refreshTokenRepository.revokeAllByUser(user);
    saveRefreshToken(user, refreshTokenStr);

    return new AuthResult(
      user.getUsername(),
      user.getRole().getName().name(),
      jwtToken,
      refreshTokenStr
    );
  }

  @Override
  @Transactional
  public AuthResult register(RegisterRequest req) {

    if (userRepository.existsByUsername(req.username())) {
      throw new ResourceConflictException("Usuario ya existe");
    }

    Role role = roleRepository.findByName(req.role())
      .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

    String passwordHashed = passwordEncoder.encode(req.password());

    User user = User.builder()
      .name(req.name())
      .username(req.username())
      .password(passwordHashed)
      .role(role)
      .build();

    User savedUser = userRepository.save(user);

    String jwtToken      = jwtService.generateToken(savedUser);
    String refreshTokenStr = jwtService.generateRefreshToken(savedUser);

    saveRefreshToken(savedUser, refreshTokenStr);

    return new AuthResult(
      savedUser.getUsername(),
      savedUser.getRole().getName().name(),
      jwtToken,
      refreshTokenStr
    );
  }

  @Override
  @Transactional
  public AuthResult refreshToken(String refreshTokenStr) {
    if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
      throw new UnauthorizedException("Invalid cookie token");
    }

    RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenStr)
      .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    if (!stored.isValid()) {
      throw new UnauthorizedException("Refresh token expired or revoked");
    }

    User user = stored.getUser();

    stored.revoke();
    refreshTokenRepository.save(stored);

    final String accessToken     = jwtService.generateToken(user);
    final String newRefreshToken = jwtService.generateRefreshToken(user);

    saveRefreshToken(user, newRefreshToken);

    return new AuthResult(
      user.getUsername(),
      user.getRole().getName().name(),
      accessToken,
      newRefreshToken
    );
  }

  @Override
  public List<PinLoginCandidate> listPinLoginCandidates() {
    return userRepository.findPinLoginCandidates(PIN_ELIGIBLE_ROLES).stream()
      .map(u -> new PinLoginCandidate(u.getId(), u.getName()))
      .toList();
  }

  @Override
  @Transactional
  public AuthResult pinLogin(PinLoginRequest req) {
    // Same generic-error posture as password login — but enumeration here is a non-issue by
    // design: every PIN-eligible userId is already public via listPinLoginCandidates(), the PIN
    // itself is the only secret. Still one message for "no such candidate" and "wrong PIN" so a
    // client can't distinguish a locked-but-real user from a made-up userId.
    User user = userRepository.findById(req.userId())
      .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
      .filter(u -> PIN_ELIGIBLE_ROLES.contains(u.getRole().getName()))
      .filter(u -> u.getPinHash() != null)
      .orElseThrow(() -> new UnauthorizedException("PIN incorrecto"));

    if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(LocalDateTime.now())) {
      throw new UnauthorizedException("Demasiados intentos — esperá un minuto antes de volver a intentar");
    }

    if (!passwordEncoder.matches(req.pin(), user.getPinHash())) {
      registerFailedPinAttempt(user);
      throw new UnauthorizedException("PIN incorrecto");
    }

    user.setFailedPinAttempts(0);
    user.setPinLockedUntil(null);
    userRepository.save(user);

    String jwtToken = jwtService.generateToken(user);
    String refreshTokenStr = jwtService.generateRefreshToken(user);

    refreshTokenRepository.revokeAllByUser(user);
    saveRefreshToken(user, refreshTokenStr);

    return new AuthResult(
      user.getUsername(),
      user.getRole().getName().name(),
      jwtToken,
      refreshTokenStr
    );
  }

  // A 4-digit PIN has only 10,000 combinations — RateLimitFilter's per-IP throttle on
  // /auth/pin-login covers the network angle, this covers the per-account angle (two staff
  // sharing a network segment shouldn't be able to brute-force each other's PIN either).
  private void registerFailedPinAttempt(User user) {
    int attempts = user.getFailedPinAttempts() + 1;
    if (attempts >= MAX_PIN_ATTEMPTS) {
      user.setFailedPinAttempts(0);
      user.setPinLockedUntil(LocalDateTime.now().plusMinutes(PIN_LOCKOUT_MINUTES));
    } else {
      user.setFailedPinAttempts(attempts);
    }
    userRepository.save(user);
  }

  private void saveRefreshToken(User user, String tokenStr) {
    RefreshToken refreshToken = RefreshToken.builder()
      .token(tokenStr)
      .user(user)
      .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000L))
      .build();
    refreshTokenRepository.save(refreshToken);
  }
}
