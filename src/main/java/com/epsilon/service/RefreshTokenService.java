package com.epsilon.service;

import com.epsilon.entity.RefreshToken;
import com.epsilon.entity.User;
import com.epsilon.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the DB-backed refresh token lifecycle.
 *
 * Responsibilities:
 *   createRefreshToken   — issue a new token for a user after login/register.
 *   validateRefreshToken — check token exists, is not revoked, is not expired.
 *   rotateRefreshToken   — revoke old token + issue new one atomically.
 *   revokeRefreshToken   — revoke a single token on logout.
 *   revokeAllUserTokens  — revoke all tokens for a user (logout all devices).
 *
 * Config:
 *   jwt.refresh-expiration-ms → REFRESH_EXPIRATION_MS (default: 604800000 = 7 days)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    // ── Token Issuance ────────────────────────────────────────────────────────

    /**
     * Creates and persists a new refresh token for the given user.
     * Token value is a UUID string — opaque, not a JWT.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(token);
        log.debug("Issued refresh token for user ID {}", user.getId());
        return saved;
    }

    // ── Token Validation ──────────────────────────────────────────────────────

    /**
     * Validates the token string against the DB.
     * Returns an empty Optional (not an exception) so AuthService can
     * return a clean 401 without exception-based flow control.
     *
     * Checks:
     *   1. Token exists in the refresh_tokens table.
     *   2. revoked == false.
     *   3. expiryDate is in the future.
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> {
                    if (rt.isRevoked()) {
                        log.warn("Attempted use of revoked refresh token for user ID {}",
                                rt.getUser().getId());
                        return false;
                    }
                    if (rt.getExpiryDate().isBefore(Instant.now())) {
                        log.warn("Expired refresh token used for user ID {}",
                                rt.getUser().getId());
                        return false;
                    }
                    return true;
                });
    }

    // ── Token Rotation ────────────────────────────────────────────────────────

    /**
     * Atomically revokes the old token and issues a new one for the same user.
     *
     * Called on every /auth/refresh request.
     * Result: each refresh token is single-use — presenting it twice will
     * fail on the second attempt, which is the correct security behaviour.
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken existingToken) {
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);
        log.debug("Rotated refresh token for user ID {}", existingToken.getUser().getId());
        return createRefreshToken(existingToken.getUser());
    }

    // ── Token Revocation ──────────────────────────────────────────────────────

    /**
     * Revokes a single refresh token by its string value.
     * Idempotent — safe to call if the token doesn't exist.
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.debug("Revoked refresh token for user ID {}", rt.getUser().getId());
        });
    }

    /**
     * Revokes ALL active refresh tokens for a user.
     * Use for "logout all devices" or on detection of a security event.
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Revoked {} active refresh token(s) for user ID {}", count, userId);
    }
}