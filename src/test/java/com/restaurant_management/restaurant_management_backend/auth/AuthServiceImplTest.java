package com.restaurant_management.restaurant_management_backend.auth;

import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.auth.entity.RefreshToken;
import com.restaurant_management.restaurant_management_backend.auth.entity.Role;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.enums.RoleName;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceConflictException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.UnauthorizedException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock UserRepository userRepository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock JwtService jwtService;
  @Mock AuthenticationManager authenticationManager;
  @Mock BCryptPasswordEncoder bCryptPasswordEncoder;
  @Mock RoleRepository roleRepository;
  @Mock RefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  AuthServiceImpl authService;

  private Role adminRole() {
    return Role.builder().id(1L).name(RoleName.ADMIN).build();
  }

  private User adminUser() {
    return User.builder()
      .id(1L)
      .name("Admin")
      .username("admin")
      .password("encoded")
      .role(adminRole())
      .build();
  }

  private RefreshToken validRefreshToken(User user) {
    return RefreshToken.builder()
      .id(1L)
      .token("valid-token")
      .user(user)
      .expiresAt(LocalDateTime.now().plusDays(7))
      .build();
  }

  // ── login ────────────────────────────────────────────────────────────────────

  @Test
  void login_throwsResourceNotFoundWhenUserDoesNotExist() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "pw")))
      .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void login_throwsUnauthorizedWhenPasswordIncorrect() {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser()));
    when(bCryptPasswordEncoder.matches("wrong", "encoded")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void login_returnsTokensWhenCredentialsValid() {
    User user = adminUser();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
    when(bCryptPasswordEncoder.matches("password", "encoded")).thenReturn(true);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenReturn(null);
    when(jwtService.generateToken(user)).thenReturn("access-token");
    when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

    AuthResult result = authService.login(new LoginRequest("admin", "password"));

    assertThat(result.token()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.username()).isEqualTo("admin");
    assertThat(result.role()).isEqualTo(RoleName.ADMIN.name());
    verify(refreshTokenRepository).revokeAllByUser(user);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  // ── register ─────────────────────────────────────────────────────────────────

  @Test
  void register_throwsConflictWhenUsernameAlreadyExists() {
    when(userRepository.existsByUsername("admin")).thenReturn(true);

    assertThatThrownBy(() ->
      authService.register(new RegisterRequest("Admin", "admin", "pw", RoleName.ADMIN)))
      .isInstanceOf(ResourceConflictException.class);
  }

  @Test
  void register_createsUserAndReturnsTokens() {
    Role role = adminRole();
    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(roleRepository.findByName(RoleName.WAITER)).thenReturn(Optional.of(role));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      u.setId(99L);
      return u;
    });
    when(jwtService.generateToken(any())).thenReturn("new-access");
    when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

    AuthResult result = authService.register(new RegisterRequest("New User", "newuser", "pw", RoleName.WAITER));

    assertThat(result.username()).isEqualTo("newuser");
    assertThat(result.token()).isEqualTo("new-access");
    verify(userRepository).save(any(User.class));
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  // ── refreshToken ─────────────────────────────────────────────────────────────

  @Test
  void refreshToken_throwsWhenTokenIsNull() {
    assertThatThrownBy(() -> authService.refreshToken(null))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void refreshToken_throwsWhenTokenIsBlank() {
    assertThatThrownBy(() -> authService.refreshToken("   "))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void refreshToken_throwsWhenTokenNotInDb() {
    when(refreshTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refreshToken("bad-token"))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void refreshToken_throwsWhenTokenIsRevoked() {
    User user = adminUser();
    RefreshToken revoked = validRefreshToken(user);
    revoked.revoke();
    when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revoked));

    assertThatThrownBy(() -> authService.refreshToken("revoked-token"))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void refreshToken_throwsWhenTokenIsExpired() {
    User user = adminUser();
    RefreshToken expired = RefreshToken.builder()
      .id(2L).token("expired-token").user(user)
      .expiresAt(LocalDateTime.now().minusSeconds(1))
      .build();
    when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> authService.refreshToken("expired-token"))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void refreshToken_returnsNewTokensWhenValid() {
    User user = adminUser();
    RefreshToken stored = validRefreshToken(user);
    when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(stored));
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jwtService.generateToken(user)).thenReturn("new-access");
    when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

    AuthResult result = authService.refreshToken("valid-token");

    assertThat(result.token()).isEqualTo("new-access");
    assertThat(result.refreshToken()).isEqualTo("new-refresh");
    assertThat(result.username()).isEqualTo("admin");
    assertThat(stored.isRevoked()).isTrue();
  }
}
