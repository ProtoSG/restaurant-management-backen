package com.restaurant_management.restaurant_management_backend.integration;

import com.restaurant_management.restaurant_management_backend.auth.AuthService;
import com.restaurant_management.restaurant_management_backend.auth.UserRepository;
import com.restaurant_management.restaurant_management_backend.auth.dto.internal.AuthResult;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.LoginRequest;
import com.restaurant_management.restaurant_management_backend.auth.dto.request.RegisterRequest;
import com.restaurant_management.restaurant_management_backend.shared.enums.RoleName;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.ResourceNotFoundException;
import com.restaurant_management.restaurant_management_backend.shared.exceptions.UnauthorizedException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

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
        assertThatThrownBy(() ->
            authService.login(new LoginRequest("nobody_xyz", "pass")))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void login_throwsWhenPasswordIsWrong() {
        assertThatThrownBy(() ->
            authService.login(new LoginRequest("admin", "wrong-password")))
            .isInstanceOf(UnauthorizedException.class);
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
}
