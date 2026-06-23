package com.epsilon.service;

import com.epsilon.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Stateless JWT utility service.
 *
 * Responsibilities:
 *  - Generate signed access tokens (HS256) embedding userId, email, role, and jti.
 *  - Validate tokens: signature + expiry + subject match.
 *  - Extract individual claims for downstream use (filter, AuthService).
 *
 * No DB access. This service is purely CPU-bound and safe to call
 * on every request without incurring a Neon connection round-trip.
 *
 * Config (set in Render environment variables):
 *   JWT_SECRET       — HS256 signing key (min 32 chars, ideally 64-char hex)
 *   JWT_EXPIRATION_MS — Access token TTL in ms (default: 900000 = 15 min)
 */
@Service
public class JwtService {

    @Value("${JWT_SECRET}")
    private String secretKey;

    @Value("${JWT_EXPIRATION_MS:900000}")
    private long jwtExpirationMs;

    // -------------------------------------------------------------------------
    // Token Generation
    // -------------------------------------------------------------------------

    /**
     * Generate an access token for the given user.
     * Embeds: sub (email), userId, role, jti (unique token ID for future blacklist).
     *
     * @param user The authenticated user entity
     * @return Signed JWT string
     */
    public String generateAccessToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("jti", UUID.randomUUID().toString()); // unique token ID — used in Phase 3C blacklist
        return buildToken(extraClaims, user.getEmail(), jwtExpirationMs);
    }

    /**
     * Internal token builder. All token creation flows through here.
     *
     * @param extraClaims  Additional claims to embed (userId, role, jti)
     * @param subject      The token subject — always the user's email
     * @param expirationMs TTL in milliseconds
     * @return Signed compact JWT string
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token Validation
    // -------------------------------------------------------------------------

    /**
     * Validates token: checks signature, expiry, and that the subject
     * matches the provided UserDetails email.
     *
     * @param token       The JWT string from the Authorization header
     * @param userDetails Loaded UserDetails (one DB call in the filter)
     * @return true if valid
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // -------------------------------------------------------------------------
    // Claim Extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts the email (subject) from the token.
     * Used by JwtAuthenticationFilter to look up the user.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the userId long claim embedded at generation time.
     * Used by service-layer ownership checks after the filter sets SecurityContext.
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extracts the jti (JWT ID) claim.
     * Used in Phase 3C to blacklist access tokens on logout.
     */
    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    /**
     * Generic claim extractor. All specific extractors delegate here.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parses and verifies the JWT signature, returning all claims.
     * Throws JwtException subtypes (ExpiredJwtException, MalformedJwtException, etc.)
     * which the filter translates to 401 responses.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Derives the signing Key from the configured secret string.
     * HMAC-SHA256 requires at least 256 bits (32 bytes).
     * The Render env var JWT_SECRET should be a 64-char hex string for safety.
     */
    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}