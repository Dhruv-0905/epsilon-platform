package com.epsilon.dto.transaction;

import com.epsilon.enums.Currency;
import com.epsilon.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for creating a transaction.
 */
@Data
public class TransactionCreateRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    private Currency currency;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    private String description;
    
    private Long fromAccountId;
    
    private Long toAccountId;
    
    private Long categoryId;
    
    private LocalDateTime transactionDate;
}
