package com.epsilon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Persisted refresh token record in NeonDB.
 *
 * Why this exists:
 * Access tokens (JWTs) are stateless — validity is proven by signature alone,
 * so no DB row is needed per request. Refresh tokens however MUST be
 * revocable (logout, breach response), so each one gets a DB row.
 *
 * Lifecycle:
 *   login/register → row created, revoked=false
 *   /auth/refresh  → old row marked revoked=true, new row created (rotation)
 *   /auth/logout   → row marked revoked=true
 *
 * We mark-as-revoked rather than delete so audit history is preserved.
 * A cleanup job (Phase 3D) can purge expired+revoked rows periodically.
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The opaque token string sent to the client.
     * UUID-format string — NOT a JWT.
     * Stored as plain text; it is meaningless without its DB record.
     */
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    /**
     * The user this token belongs to.
     * LAZY fetch — we only need the User when validating a token,
     * not on every load of a RefreshToken.
     * No cascade — token lifecycle is managed explicitly, not via User.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Absolute expiry as a UTC Instant.
     * Instant is used (not LocalDateTime) to avoid timezone ambiguity —
     * both Render and NeonDB run in UTC.
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Revocation flag.
     * true  = token can no longer be used.
     * false = token is active (subject to expiry check).
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}