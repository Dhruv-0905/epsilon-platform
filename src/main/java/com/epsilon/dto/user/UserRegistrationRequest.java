package com.epsilon.dto.user;

import com.epsilon.enums.Currency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for user registration.
 */
@Data
public class UserRegistrationRequest {
   
    @NotBlank(message = "First Name is Required")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last Name is Required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Email is Required")
    @Email(message = "Email format is invalid")
    private String email;

    @NotBlank(message = "Password is Required")
    @Size(min = 8, message = "Password must be atleast 8 characters")
    private String passwordHash;

    private Currency defaultCurrency;
}
