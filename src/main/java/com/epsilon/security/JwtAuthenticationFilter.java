package com.epsilon.security;

import com.epsilon.service.EpsilonUserDetailsService;
import com.epsilon.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — runs once per HTTP request, before Spring Security's
 * own UsernamePasswordAuthenticationFilter in the filter chain.
 *
 * Responsibilities:
 *   1. Extract the Bearer token from the Authorization header.
 *   2. Validate the token (signature + expiry) via JwtService — NO DB hit.
 *   3. Load UserDetails via EpsilonUserDetailsService — ONE indexed DB hit.
 *   4. Populate SecurityContextHolder so the rest of the request is authenticated.
 *
 * What this filter does NOT do:
 *   - It does not throw exceptions. On any failure (missing header, bad token,
 *     expired token) it simply calls filterChain.doFilter() without setting
 *     the SecurityContext. Spring Security's AuthenticationEntryPoint then
 *     handles the unauthenticated request and returns the 401 JSON response
 *     configured in SecurityConfig.
 *   - It does not handle path matching. SecurityConfig's permitAll() rules
 *     allow /api/auth/** through without authentication — this filter still
 *     runs on those paths but finding no valid token it simply passes through.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final EpsilonUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // ── Step 1: Guard — skip if no Bearer token present ───────────────────
        // Public endpoints (/api/auth/**, /actuator/health) will reach here
        // with no Authorization header. We simply continue the chain.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Extract and validate the JWT ──────────────────────────────
        final String jwt = authHeader.substring(7); // strip "Bearer " prefix
        final String userEmail;

        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Covers ExpiredJwtException, MalformedJwtException, SignatureException, etc.
            // Log at debug — this will happen frequently for expired tokens and is not an error.
            log.debug("JWT extraction failed for request to {}: {}", request.getRequestURI(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 3: Authenticate if not already in SecurityContext ────────────
        // The null check prevents re-authenticating within the same request
        // (e.g. if another filter already set the context).
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(userEmail);
            } catch (Exception e) {
                // User was deleted after token was issued — treat as unauthenticated.
                log.debug("UserDetails load failed for email {}: {}", userEmail, e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            // Final validation: confirm signature matches AND token is not expired.
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials null — password not needed post-auth
                                userDetails.getAuthorities()   // carries ROLE_USER / ROLE_ADMIN
                        );

                // Attach request metadata (remote IP, session ID) to the auth token.
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set in SecurityContext — from this point the request is authenticated.
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user {} for {}", userEmail, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }
}