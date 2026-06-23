package com.epsilon.controller;

import com.epsilon.dto.ApiResponse;
import com.epsilon.dto.auth.AuthResponse;
import com.epsilon.dto.auth.LoginRequest;
import com.epsilon.dto.auth.RefreshRequest;
import com.epsilon.dto.auth.RegisterRequest;
import com.epsilon.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints for Epsilon.
 *
 * All paths under /api/auth/** are declared PUBLIC in SecurityConfig —
 * no Bearer token is required to reach these endpoints.
 *
 * Error handling:
 *   - @Valid failures   → GlobalExceptionHandler → 400 with field errors
 *   - Duplicate email   → AuthService throws IllegalArgumentException
 *                       → GlobalExceptionHandler → 400
 *   - Bad credentials   → AuthService throws BadCredentialsException
 *                       → handleAuthException below → 401
 *   - Invalid refresh   → AuthService throws IllegalArgumentException
 *                       → GlobalExceptionHandler → 400
 *
 * The existing GlobalExceptionHandler already covers IllegalArgumentException
 * (returns 400). The one new handler added here is for Spring Security's
 * BadCredentialsException (login failure → 401, not 400).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Register, login, refresh tokens, and logout")
public class AuthController {

    private final AuthService authService;

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new ROLE_USER account and returns a dual access+refresh token pair."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Register request for email: {}", request.getEmail());
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully.", authResponse));
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(
        summary = "Login with email and password",
        description = "Authenticates credentials and returns a dual access+refresh token pair."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", authResponse));
    }

    // ── POST /api/auth/refresh ────────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Exchanges a valid refresh token for a new access+refresh token pair. " +
                      "The old refresh token is immediately invalidated (rotation)."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {

        AuthResponse authResponse = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully.", authResponse));
    }

    // ── POST /api/auth/logout ─────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
        summary = "Logout",
        description = "Revokes the provided refresh token. " +
                      "The access token will naturally expire within its TTL (15 min). " +
                      "Client should discard both tokens on receipt of this response."
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }

    // ── Exception handler: login failures ────────────────────────────────────

    /**
     * Spring Security throws BadCredentialsException (a subclass of
     * AuthenticationException, NOT of IllegalArgumentException) when
     * email or password is wrong. The GlobalExceptionHandler's generic
     * Exception handler would catch it as a 500 — wrong status code for
     * a login failure. This handler catches it specifically and returns 401.
     *
     * The message is deliberately generic: "Invalid credentials."
     * Never say "email not found" vs "wrong password" — that leaks
     * information about registered emails (user enumeration attack).
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(
            org.springframework.security.core.AuthenticationException ex) {

        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials."));
    }
}