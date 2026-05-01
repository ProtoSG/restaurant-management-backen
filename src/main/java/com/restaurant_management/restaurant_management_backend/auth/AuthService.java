package com.restaurant_management.restaurant_management_backend.auth;

import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;

public interface AuthService {

  AuthResult login(LoginRequest req);
  AuthResult register(RegisterRequest req);
  AuthResult refreshToken(final String refreshToken);

}
