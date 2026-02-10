package com.epsilon.dto.transaction;

import com.epsilon.enums.Currency;
import com.epsilon.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transaction response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private Currency currency;
    private TransactionType transactionType;
    private String description;
    private Long fromAccountId;
    private String fromAccountName;
    private Long toAccountId;
    private String toAccountName;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime transactionDate;
    private LocalDateTime createdAt;
}
