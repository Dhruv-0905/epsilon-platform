package com.epsilon.dto.user;

import com.epsilon.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user response.
 * Excludes sensitive data like password.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse{
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Currency defaultCurrency;
    private Boolean isActive;
    private LocalDateTime createdAt;
}