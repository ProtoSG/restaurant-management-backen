package com.restaurant_management.restaurant_management_backend.auth;

import java.util.List;

import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.PinLoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.response.PinLoginCandidate;

public interface AuthService {

  AuthResult login(LoginRequest req);
  AuthResult register(RegisterRequest req);
  AuthResult refreshToken(final String refreshToken);
  List<PinLoginCandidate> listPinLoginCandidates();
  AuthResult pinLogin(PinLoginRequest req);

}
