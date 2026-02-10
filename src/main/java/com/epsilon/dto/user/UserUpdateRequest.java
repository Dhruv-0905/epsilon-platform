package com.epsilon.dto.user;

import com.epsilon.enums.Currency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating user profile.
 */
@Data
public class UserUpdateRequest {
    
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;
    
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private Currency defaultCurrency;
}
