package com.epsilon.enums;

/**
 * User roles for Epsilon RBAC.
 *
 * ROLE_USER  — standard authenticated user; can only access their own data.
 * ROLE_ADMIN — elevated access; can reach /api/admin/** endpoints and
 *              view any user's data (Phase 3D).
 *
 * Prefixed with ROLE_ because Spring Security's hasRole("ADMIN") check
 * automatically prepends ROLE_ when matching — keeping the prefix explicit
 * here makes the mapping unambiguous.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}