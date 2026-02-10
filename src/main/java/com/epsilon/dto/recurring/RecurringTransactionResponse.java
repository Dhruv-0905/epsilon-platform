package com.epsilon.dto.recurring;

import com.epsilon.enums.Currency;
import com.epsilon.enums.RecurringFrequency;
import com.epsilon.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for recurring transaction response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionResponse {
    private Long id;
    private Long accountId;
    private String accountName;
    private BigDecimal amount;
    private Currency currency;
    private TransactionType transactionType;
    private RecurringFrequency frequency;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextRunDate;
    private Long categoryId;
    private String categoryName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
