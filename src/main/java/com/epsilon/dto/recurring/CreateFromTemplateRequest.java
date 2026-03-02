package com.epsilon.dto.recurring;

import com.epsilon.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating a recurring transaction from a pre-defined template.
 * The template supplies transactionType and frequency — user supplies amount,
 * currency, account and dates.
 */
@Data
public class CreateFromTemplateRequest {

    /** Template identifier — see GET /api/recurring-transactions/templates for valid IDs. */
    @NotBlank(message = "Template ID is required")
    private String templateId;

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;      // Optional — null means runs forever

    private Long categoryId;        // Optional
}
