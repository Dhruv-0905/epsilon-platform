package com.epsilon.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body returned by /register, /login, and /refresh.
 *
 * accessToken  — short-lived JWT (15 min by default).
 *                Client sends this as: Authorization: Bearer <token>
 *                on every protected API call.
 *
 * refreshToken — long-lived opaque UUID (7 days by default).
 *                Client stores this securely and sends it only
 *                to POST /api/auth/refresh to get a new token pair.
 *
 * tokenType    — always "Bearer" (standard OAuth2 convention).
 *
 * Note: userId and role are NOT included here.
 * They are embedded as private claims inside the JWT itself,
 * readable by the client via base64-decode of the JWT payload.
 * No need to expose them separately in this response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
}