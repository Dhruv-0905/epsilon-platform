package com.epsilon.dto.account;

import com.epsilon.enums.AccountType;
import com.epsilon.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for account creation.
 */
@Data
public class AccountCreateRequest{
    @NotBlank(message = "Account name is required")
    private String accountName;
    
    @NotNull(message = "Account type is required")
    private AccountType accountType;
    
    private Currency currency;
    
    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    private BigDecimal balance;
    
    private String bankName;
}