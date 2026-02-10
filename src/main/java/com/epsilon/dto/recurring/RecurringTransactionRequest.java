package com.epsilon.dto.recurring;

import com.epsilon.enums.Currency;
import com.epsilon.enums.RecurringFrequency;
import com.epsilon.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for recurring transaction creation/update.
 */
@Data
public class RecurringTransactionRequest {
    
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    private Currency currency;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    @NotNull(message = "Frequency is required")
    private RecurringFrequency frequency;
    
    private String description;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Long categoryId;
}
