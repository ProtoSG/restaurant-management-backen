package com.restaurant_management.restaurant_management_backend.auth;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.auth.entity.RefreshToken;
import com.restaurant_management.restaurant_management_backend.auth.entity.Role;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.UnauthorizedException;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshTokenExpiration;

  @Override
  @Transactional
  public AuthResult login(LoginRequest req) {

    User user = userRepository.findByUsername(req.username())
      .orElseThrow(() -> new ResourceNotFoundException("Este usuario no existe"));

    if (!bCryptPasswordEncoder.matches(req.password(), user.getPassword())) {
      throw new UnauthorizedException("Contraseña incorrecta");
    }

    authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        req.username(),
        req.password()
      )
    );

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

  private void saveRefreshToken(User user, String tokenStr) {
    RefreshToken refreshToken = RefreshToken.builder()
      .token(tokenStr)
      .user(user)
      .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000L))
      .build();
    refreshTokenRepository.save(refreshToken);
  }
}
