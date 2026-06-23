package com.epsilon.config;

import com.epsilon.security.JwtAuthenticationFilter;
import com.epsilon.service.EpsilonUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

/**
 * Central Spring Security configuration for Epsilon.
 *
 * Key decisions:
 *
 * 1. STATELESS sessions — no HttpSession is ever created. Every request must
 *    carry its own JWT. This keeps Render free-tier memory usage minimal and
 *    makes horizontal scaling trivial later.
 *
 * 2. CSRF disabled — Epsilon is a pure JSON API consumed by non-browser clients
 *    (Postman, mobile app, future React frontend with token-based auth). CSRF
 *    protection is only meaningful for cookie-session-based web apps.
 *
 * 3. Path rules — three tiers:
 *      PUBLIC   /api/auth/**          — register, login, refresh, logout
 *               /actuator/health      — Render health check probe
 *               /swagger-ui/**        — dev/portfolio documentation
 *               /v3/api-docs/**       — OpenAPI JSON used by Swagger UI
 *      ADMIN    /api/admin/**         — scheduler monitoring endpoints (Phase 3D)
 *      DEFAULT  everything else       — requires authenticated user (any role)
 *
 * 4. @EnableMethodSecurity — enables @PreAuthorize on service/controller methods.
 *    Required for Phase 3B ownership checks and admin-only method guards.
 *
 * 5. Inline 401/403 handlers — Spring Security's defaults return HTML error pages.
 *    These lambdas return clean JSON so Postman and API clients get readable errors.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize, @PostAuthorize, @Secured
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final EpsilonUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;   // Spring Boot auto-configures this bean

    // ── Public path whitelist ─────────────────────────────────────────────────

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**"
    };

    // ── Security Filter Chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Disable CSRF (stateless API) ──────────────────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Stateless session management ──────────────────────────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Path-based authorization rules ────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // ── Custom 401 handler — returns JSON instead of HTML ─────────────
            // Triggered when an unauthenticated request reaches a protected endpoint.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(
                        objectMapper.writeValueAsString(Map.of(
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "Authentication required. Please provide a valid Bearer token.",
                            "path", request.getRequestURI()
                        ))
                    );
                })
                // ── Custom 403 handler — returns JSON instead of HTML ─────────
                // Triggered when an authenticated user lacks the required role.
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(
                        objectMapper.writeValueAsString(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "You do not have permission to access this resource.",
                            "path", request.getRequestURI()
                        ))
                    );
                })
            )

            // ── Wire authentication provider ──────────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── Register JWT filter BEFORE Spring's username/password filter ──
            // This ensures every request with a valid Bearer token is authenticated
            // before Spring Security's own filter checks access rules.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Beans ─────────────────────────────────────────────────────────────────

    /**
     * Wires EpsilonUserDetailsService + BCryptPasswordEncoder into Spring Security's
     * DaoAuthenticationProvider, which AuthenticationManager delegates to during login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes AuthenticationManager as a bean so AuthService can inject it
     * and call authenticate() during the login flow.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * BCrypt with strength 10.
     *
     * Strength 10 = ~100ms per hash on modern hardware.
     * On Render's shared free-tier CPU this is slightly slower (~150–200ms),
     * which is still perfectly acceptable for a login endpoint and provides
     * solid brute-force resistance without pegging the CPU.
     * Strength 12+ would be ~400ms+ and noticeably sluggish on free tier.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}