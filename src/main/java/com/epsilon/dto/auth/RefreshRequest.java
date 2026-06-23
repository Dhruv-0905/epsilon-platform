package com.epsilon.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for POST /api/auth/refresh.
 * Client sends the refresh token string it received at login/register.
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}