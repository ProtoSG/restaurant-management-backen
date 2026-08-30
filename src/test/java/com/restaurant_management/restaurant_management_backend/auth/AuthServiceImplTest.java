package com.restaurant_management.restaurant_management_backend.auth;

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
import com.restaurant_management.restaurant_management_backend.shared.exceptions.UnauthorizedException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock UserRepository userRepository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock JwtService jwtService;
  @Mock AuthenticationManager authenticationManager;
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

  private Role waiterRole() {
    return Role.builder().id(2L).name(RoleName.WAITER).build();
  }

  private User waiterUser() {
    return waiterUser(0, null);
  }

  private User waiterUser(int failedPinAttempts, LocalDateTime pinLockedUntil) {
    return User.builder()
      .id(10L)
      .name("Mesero Juan")
      .username("juan")
      .password("encoded")
      .role(waiterRole())
      .isActive(true)
      .pinHash("encoded-pin")
      .failedPinAttempts(failedPinAttempts)
      .pinLockedUntil(pinLockedUntil)
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
  void login_throwsUnauthorizedWithGenericMessageWhenUserDoesNotExist() {
    // DaoAuthenticationProvider hides "user not found" as BadCredentials
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "pw")))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Usuario o contraseña incorrectos");
  }

  @Test
  void login_throwsUnauthorizedWhenPasswordIncorrect() {
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Usuario o contraseña incorrectos");
  }

  @Test
  void login_returnsTokensWhenCredentialsValid() {
    User user = adminUser();
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenReturn(null);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
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

  // ── listPinLoginCandidates ──────────────────────────────────────────────────

  @Test
  void listPinLoginCandidates_mapsEligibleUsersToNameOnly() {
    User waiter = waiterUser();
    when(userRepository.findPinLoginCandidates(List.of(RoleName.WAITER, RoleName.CASHIER)))
      .thenReturn(List.of(waiter));

    List<PinLoginCandidate> candidates = authService.listPinLoginCandidates();

    assertThat(candidates).containsExactly(new PinLoginCandidate(10L, "Mesero Juan"));
  }

  // ── pinLogin ─────────────────────────────────────────────────────────────────

  @Test
  void pinLogin_returnsTokensWhenPinCorrect() {
    User waiter = waiterUser();
    when(userRepository.findById(10L)).thenReturn(Optional.of(waiter));
    when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);
    when(jwtService.generateToken(waiter)).thenReturn("pin-access");
    when(jwtService.generateRefreshToken(waiter)).thenReturn("pin-refresh");
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

    AuthResult result = authService.pinLogin(new PinLoginRequest(10L, "1234"));

    assertThat(result.token()).isEqualTo("pin-access");
    assertThat(result.username()).isEqualTo("juan");
    assertThat(result.role()).isEqualTo(RoleName.WAITER.name());
    assertThat(waiter.getFailedPinAttempts()).isZero();
    assertThat(waiter.getPinLockedUntil()).isNull();
    verify(refreshTokenRepository).revokeAllByUser(waiter);
  }

  @Test
  void pinLogin_throwsGenericMessageWhenUserIdDoesNotExist() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(999L, "1234")))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("PIN incorrecto");
  }

  @Test
  void pinLogin_throwsGenericMessageWhenRoleNotEligible() {
    // Defense in depth — even if an ADMIN somehow ended up with a pinHash set (should never
    // happen via UserController.setPin's own role guard), AuthServiceImpl still refuses.
    User admin = adminUser();
    admin.setPinHash("encoded-pin");
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

    assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(1L, "1234")))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("PIN incorrecto");
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  void pinLogin_throwsGenericMessageWhenUserHasNoPinConfigured() {
    User waiter = waiterUser();
    waiter.setPinHash(null);
    when(userRepository.findById(10L)).thenReturn(Optional.of(waiter));

    assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(10L, "1234")))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("PIN incorrecto");
  }

  @Test
  void pinLogin_throwsGenericMessageWhenUserIsInactive() {
    User waiter = waiterUser();
    waiter.setIsActive(false);
    when(userRepository.findById(10L)).thenReturn(Optional.of(waiter));

    assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(10L, "1234")))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("PIN incorrecto");
  }

  @Test
  void pinLogin_throwsWhenPinWrong_andIncrementsFailedAttempts() {
    User waiter = waiterUser(2, null);
    when(userRepository.findById(10L)).thenReturn(Optional.of(waiter));
    when(passwordEncoder.matches("0000", "encoded-pin")).thenReturn(false);

    assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(10L, "0000")))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("PIN incorrecto");

    assertThat(waiter.getFailedPinAttempts()).isEqualTo(3);
    assertThat(waiter.getPinLockedUntil()).isNull();
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void pinLogin_locksAccountAfterFifthFailedAttempt() {
    User waiter = waiterUser(4, null); // one more wrong attempt reaches MAX_PIN_ATTEMPTS (5)
    when(userRepository.findById(10L)).thenReturn(Optional.of(waiter));
    when(passwordEncoder.matches("0000", "encoded-pin")).thenReturn(false);

    assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(10L, "0000")))
      .isInstanceOf(UnauthorizedException.class);

    // Counter resets and a fresh lockout window starts, rather than growing unboundedly.
    assertThat(waiter.getFailedPinAttempts()).isZero();
    assertThat(waiter.getPinLockedUntil()).isAfter(LocalDateTime.now());
  }

  @Test
  void pinLogin_throwsWhenLockedOut_evenWithTheCorrectPin() {
    User waiter = waiterUser(0, LocalDateTime.now().plusSeconds(30));
    when(userRepository.findById(10L)).thenReturn(Optional.of(waiter));

    assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(10L, "1234")))
      .isInstanceOf(UnauthorizedException.class);

    // Locked out short-circuits before ever checking the PIN — a correct guess during lockout
    // must not succeed, and must not reset the lock early either.
    verifyNoInteractions(passwordEncoder);
    assertThat(waiter.getPinLockedUntil()).isNotNull();
  }

  @Test
  void pinLogin_allowsRetryOncePastLockoutWindow() {
    User waiter = waiterUser(0, LocalDateTime.now().minusSeconds(1)); // lock just expired
    when(userRepository.findById(10L)).thenReturn(Optional.of(waiter));
    when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);
    when(jwtService.generateToken(waiter)).thenReturn("access");
    when(jwtService.generateRefreshToken(waiter)).thenReturn("refresh");
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

    AuthResult result = authService.pinLogin(new PinLoginRequest(10L, "1234"));

    assertThat(result.token()).isEqualTo("access");
  }
}
