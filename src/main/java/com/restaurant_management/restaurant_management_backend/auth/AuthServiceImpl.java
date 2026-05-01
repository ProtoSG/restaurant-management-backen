package com.restaurant_management.restaurant_management_backend.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.auth.entity.Role;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.UnauthorizedException;

import jakarta.transaction.Transactional;
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

  @Override
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

    String jwtToken     = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    return new AuthResult(
      user.getUsername(),
      user.getRole().getName().name(),
      jwtToken,
      refreshToken
    );

  }

  @Override
  @Transactional
  public AuthResult register(RegisterRequest req) {

    if (userRepository.existsByUsername(req.username())) {
      throw new ResourceConflictException("Usuario ya existe");
    }

    Role role = roleRepository.findByName(req.role())
      .orElseThrow(() -> new ResourceNotFoundException("Role nor found"));

    String passwordHashed = passwordEncoder.encode(req.password());

    User user = User.builder()
      .name(req.name())
      .username(req.username())
      .password(passwordHashed)
      .role(role)
      .build();

    User savedUser = userRepository.save(user);

    String jwtToken = jwtService.generateToken(savedUser);
    String refreshToken = jwtService.generateRefreshToken(savedUser);

    return new AuthResult(
      savedUser.getUsername(),
      savedUser.getRole().getName().name(),
      jwtToken,
      refreshToken
    );

  }
  @Override
  public AuthResult refreshToken(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new UnauthorizedException("Invalid cookie token");
    }

    final String username = jwtService.extractUsername(refreshToken);

    if(username == null) {
      throw new UnauthorizedException("Invalid refresh token");
    }

    final User user = userRepository.findByUsername(username)
      .orElseThrow(() -> new ResourceNotFoundException("Usuario con username: '" + username + "' no existe"));

    if (!jwtService.isTokenValid(refreshToken, user)) {
      throw new UnauthorizedException("Invalid refresh token");
    }

    final String accessToken = jwtService.generateToken(user);
    final String newRefreshToken = jwtService.generateRefreshToken(user);

    return new AuthResult(
      user.getUsername(),
      user.getRole().getName().name(),
      accessToken,
      newRefreshToken
    );
  }

}
