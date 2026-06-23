package com.epsilon.entity;

import com.epsilon.enums.Currency;
import com.epsilon.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is a Mandatory Field")
    @Size(max = 50)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is Mandatory Field")
    @Size(max = 50)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Email(message = "Enter a valid Email")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is Required")
    @Size(min = 8, message = "Password should be at least 8 characters long")
    @Column(nullable = false)
    private String passwordHash;

    // ── NEW: RBAC role ─────────────────────────────────────────────────────
    // Stored as a plain string in DB (e.g. "ROLE_USER").
    // ddl-auto: update will ADD this column to the existing users table
    // without dropping any data.
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Default Currency is required")
    @Column(name = "default_currency", nullable = false)
    @Builder.Default
    private Currency defaultCurrency = Currency.USD;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> categories = new ArrayList<>();

    // ── UserDetails implementation ─────────────────────────────────────────
    // Spring Security calls these during every authenticated request.
    // We implement directly on the entity to avoid a separate adapter class.

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Spring Security uses getUsername() as the principal identifier.
     * Epsilon uses email as the unique login key — not a separate username field.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Returns the BCrypt-hashed password.
     * Spring Security compares this with the raw password submitted at login.
     * The field is named passwordHash in the DB; this method bridges it to
     * the UserDetails contract which expects getPassword().
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * Maps to the existing isActive flag on the entity.
     * If an admin deactivates a user in future, their JWTs will be rejected
     * at the UserDetails load step without needing token revocation.
     */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }
}