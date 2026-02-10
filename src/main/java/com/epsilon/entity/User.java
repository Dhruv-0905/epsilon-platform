package com.epsilon.entity;

import com.epsilon.enums.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.lang.annotation.Inherited;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User{
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
    @Size(min = 8, message = "Password should be 8 at least characters long")
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Default Currency is required")
    @Column(name = "default_currency", nullable = false)
    private Currency defaultCurrency = Currency.USD;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "user" ,cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> categories = new ArrayList<>();

}



