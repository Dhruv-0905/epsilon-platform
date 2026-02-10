package com.epsilon.dto.account;

import com.epsilon.enums.AccountType;
import com.epsilon.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for account response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String accountName;
    private String accountNumber;
    private AccountType accountType;
    private Currency currency;
    private BigDecimal balance;
    private String bankName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
