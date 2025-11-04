package com.restaurant_management.restaurant_management_backend.service;

import com.restaurant_management.restaurant_management_backend.dto.AuthRequestDTO;
import com.restaurant_management.restaurant_management_backend.dto.AuthResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.UserCreateDTO;

public interface AuthService {

  AuthResponseDTO login(AuthRequestDTO authRequestDTO);

  AuthResponseDTO register(UserCreateDTO userCreateDTO);

  AuthResponseDTO refreshToken(final String refreshToken);
}
