package com.restaurant_management.restaurant_management_backend.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.dto.AuthRequestDTO;
import com.restaurant_management.restaurant_management_backend.dto.AuthResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.UserCreateDTO;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.enums.Role;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.exceptions.UnauthorizedException;
import com.restaurant_management.restaurant_management_backend.repository.UserRepository;
import com.restaurant_management.restaurant_management_backend.service.AuthService;
import com.restaurant_management.restaurant_management_backend.service.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
          authRequestDTO.getEmail(), 
          authRequestDTO.getPassword()
        )
      );

    User user = userRepository.findByEmail(authRequestDTO.getEmail())
      .orElseThrow();

    String jwtToken = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    return AuthResponseDTO.builder()
      .email(user.getEmail())
      .token(jwtToken)
      .refreshToken(refreshToken)
      .build();
  }

  public AuthResponseDTO register(UserCreateDTO userCreateDTO) {

    if (userRepository.existsByEmail(userCreateDTO.getEmail())) {
      throw new ResourceConflictException("Usuario con email: '" + userCreateDTO.getEmail() + "' ya existe");
    }

    User user = User.builder()
      .name(userCreateDTO.getName())
      .email(userCreateDTO.getEmail())
      .password(passwordEncoder.encode(userCreateDTO.getPassword()))
      .role(userCreateDTO.getRole())
      .build();

    User savedUser = userRepository.save(user);

    String jwtToken = jwtService.generateToken(savedUser);
    String refreshToken = jwtService.generateRefreshToken(savedUser);

    AuthResponseDTO responseDTO = AuthResponseDTO.builder()
      .email(userCreateDTO.getEmail())
      .token(jwtToken)
      .refreshToken(refreshToken)
      .build();

    return responseDTO;
  }

  public AuthResponseDTO refreshToken(final String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new UnauthorizedException("Invalid cookie token");
    }

    final String userEmail = jwtService.extractUsername(refreshToken);

    if(userEmail == null) {
      throw new UnauthorizedException("Invalid refresh token");
    }

    final User user = userRepository.findByEmail(userEmail)
      .orElseThrow(() -> new ResourceNotFoundException("Usuario con email: '" + userEmail + "' no existe"));

    if (!jwtService.isTokenValid(refreshToken, user)) {
      throw new UnauthorizedException("Invalid refresh token");
    }

    final String accessToken = jwtService.generateToken(user);
    final String newRefreshToken = jwtService.generateRefreshToken(user);

    return AuthResponseDTO.builder()
      .email(user.getEmail())
      .token(accessToken)
      .refreshToken(newRefreshToken)
      .build();
  }
}
