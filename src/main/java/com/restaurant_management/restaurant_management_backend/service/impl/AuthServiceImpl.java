package com.restaurant_management.restaurant_management_backend.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.dto.AuthRequestDTO;
import com.restaurant_management.restaurant_management_backend.dto.AuthResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.UserCreateDTO;
import com.restaurant_management.restaurant_management_backend.entity.Role;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.exceptions.UnauthorizedException;
import com.restaurant_management.restaurant_management_backend.repository.RoleRepository;
import com.restaurant_management.restaurant_management_backend.repository.UserRepository;
import com.restaurant_management.restaurant_management_backend.service.AuthService;
import com.restaurant_management.restaurant_management_backend.service.JwtService;

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
  public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {

    User user = userRepository.findByUsername(authRequestDTO.getUsername())
      .orElseThrow(() -> new ResourceNotFoundException("Este usuario no existe"));

    if (!bCryptPasswordEncoder.matches(authRequestDTO.getPassword(), user.getPassword())) {
      throw new UnauthorizedException("Contraseña incorrecta");
    }

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
          authRequestDTO.getUsername(), 
          authRequestDTO.getPassword()
        )
      );

    String jwtToken = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    return AuthResponseDTO.builder()
      .username(user.getUsername())
      .token(jwtToken)
      .refreshToken(refreshToken)
      .build();
  }

  @Override
  @Transactional
  public AuthResponseDTO register(UserCreateDTO userCreateDTO) {
    if (userRepository.existsByUsername(userCreateDTO.getUsername())) {
      throw new ResourceConflictException("Usuario con username: '" + userCreateDTO.getUsername() + "' ya existe");
    }

    Role role = roleRepository.findByName(userCreateDTO.getRole())
      .orElseThrow(() -> new ResourceNotFoundException("Role nor found"));

    String passwordHashed = passwordEncoder.encode(userCreateDTO.getPassword());

    User user = User.builder()
      .name(userCreateDTO.getName())
      .username(userCreateDTO.getUsername())
      .password(passwordHashed)
      .role(role)
      .build();

    User savedUser = userRepository.save(user);

    String jwtToken = jwtService.generateToken(savedUser);
    String refreshToken = jwtService.generateRefreshToken(savedUser);

    AuthResponseDTO responseDTO = AuthResponseDTO.builder()
      .username(userCreateDTO.getUsername())
      .token(jwtToken)
      .refreshToken(refreshToken)
      .build();

    return responseDTO;
  }

  public AuthResponseDTO refreshToken(final String refreshToken) {
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

    return AuthResponseDTO.builder()
      .username(user.getUsername())
      .token(accessToken)
      .refreshToken(newRefreshToken)
      .build();
  }
}
