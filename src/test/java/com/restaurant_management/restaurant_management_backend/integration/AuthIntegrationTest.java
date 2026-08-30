package com.restaurant_management.restaurant_management_backend.integration;

import com.restaurant_management.restaurant_management_backend.auth.AuthService;
import com.restaurant_management.restaurant_management_backend.auth.UserRepository;
import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.PinLoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.response.PinLoginCandidate;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.shared.enums.RoleName;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.UnauthorizedException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // AuthService has no "set pin" method by design (only UserController.setPin, ADMIN-only) —
    // tests reach into the repository directly to prepare a user with a PIN, same trust
    // boundary as the real ADMIN-only write path.
    private User registerWaiterWithPin(String pin) {
        String username = "waiter_" + UUID.randomUUID().toString().substring(0, 8);
        authService.register(new RegisterRequest("Test Waiter", username, "pass123", RoleName.WAITER));
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setPinHash(passwordEncoder.encode(pin));
        return userRepository.save(user);
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_persistsUserWithHashedPassword() {
        String username = "testuser_" + UUID.randomUUID().toString().substring(0, 8);

        authService.register(new RegisterRequest("Test User", username, "pass123", RoleName.WAITER));

        assertThat(userRepository.findByUsername(username)).isPresent();
        // stored password should be hashed, not plain text
        String stored = userRepository.findByUsername(username).get().getPassword();
        assertThat(stored).doesNotContain("pass123");
        assertThat(stored).startsWith("$2");  // BCrypt prefix
    }

    @Test
    void register_returnsJwtTokens() {
        String username = "testuser_" + UUID.randomUUID().toString().substring(0, 8);

        AuthResult result = authService.register(
            new RegisterRequest("Test User", username, "pass123", RoleName.WAITER));

        assertThat(result.token()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.username()).isEqualTo(username);
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_returnsTokensForValidCredentials() {
        // admin user seeded by DataInitializer at context startup
        AuthResult result = authService.login(new LoginRequest("admin", "admin123"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.role()).isEqualTo(RoleName.ADMIN.name());
    }

    @Test
    void login_throwsWhenUserDoesNotExist() {
        // Generic message + same exception as wrong-password → no username enumeration
        assertThatThrownBy(() ->
            authService.login(new LoginRequest("nobody_xyz", "pass")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void login_throwsWhenPasswordIsWrong() {
        assertThatThrownBy(() ->
            authService.login(new LoginRequest("admin", "wrong-password")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void login_afterRegister_succeeds() {
        String username = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
        authService.register(new RegisterRequest("Test", username, "secret", RoleName.WAITER));

        AuthResult result = authService.login(new LoginRequest(username, "secret"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.username()).isEqualTo(username);
    }

    // ── refreshToken ─────────────────────────────────────────────────────────

    @Test
    void refreshToken_returnsNewTokensForValidRefreshToken() {
        AuthResult initial = authService.login(new LoginRequest("admin", "admin123"));

        AuthResult refreshed = authService.refreshToken(initial.refreshToken());

        assertThat(refreshed.token()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(refreshed.username()).isEqualTo("admin");
    }

    @Test
    void refreshToken_throwsForInvalidToken() {
        assertThatThrownBy(() -> authService.refreshToken("not-a-real-token"))
            .isInstanceOf(Exception.class);
    }

    // ── PIN login ────────────────────────────────────────────────────────────

    @Test
    void pinLogin_returnsTokensForValidPin() {
        User waiter = registerWaiterWithPin("4321");

        AuthResult result = authService.pinLogin(new PinLoginRequest(waiter.getId(), "4321"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.username()).isEqualTo(waiter.getUsername());
        assertThat(result.role()).isEqualTo(RoleName.WAITER.name());
    }

    @Test
    void pinLogin_throwsForAdminEvenIfPinHashSomehowSet() {
        // Defense in depth against the AuthServiceImpl role check specifically, independent of
        // UserController.setPin's own guard — the admin seeded by DataInitializer never has a
        // PIN through the real write path, but this proves the service layer itself refuses.
        User admin = userRepository.findByUsername("admin").orElseThrow();
        admin.setPinHash(passwordEncoder.encode("9999"));
        userRepository.save(admin);

        assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(admin.getId(), "9999")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("PIN incorrecto");
    }

    @Test
    void pinLogin_locksAfterFiveWrongAttempts_thenRejectsEvenTheCorrectPin() {
        User waiter = registerWaiterWithPin("1111");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(waiter.getId(), "0000")))
                .isInstanceOf(UnauthorizedException.class);
        }

        assertThatThrownBy(() -> authService.pinLogin(new PinLoginRequest(waiter.getId(), "1111")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Demasiados intentos — esperá un minuto antes de volver a intentar");
    }

    @Test
    void listPinLoginCandidates_includesWaiterWithPin_excludesAdmin() {
        User waiter = registerWaiterWithPin("2222");

        List<PinLoginCandidate> candidates = authService.listPinLoginCandidates();

        assertThat(candidates).extracting(PinLoginCandidate::id).contains(waiter.getId());
        assertThat(candidates).extracting(PinLoginCandidate::id)
            .doesNotContain(userRepository.findByUsername("admin").orElseThrow().getId());
    }
}
