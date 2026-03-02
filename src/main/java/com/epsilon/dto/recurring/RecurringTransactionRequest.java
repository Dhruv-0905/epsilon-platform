package com.epsilon.dto.recurring;

import com.epsilon.enums.AmountType;
import com.epsilon.enums.Currency;
import com.epsilon.enums.RecurringFrequency;
import com.epsilon.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for recurring transaction creation/update.
 * Phase 2D adds: amountType, percentageValue, minimumBalanceThreshold, skipWeekends.
 */
@Data
public class RecurringTransactionRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    /**
     * For FIXED rules: the exact transaction amount.
     * For PERCENTAGE rules: acts as a safety cap (optional but recommended).
     */
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

    // ── Phase 2D ──────────────────────────────────────────────────────────────

    /**
     * FIXED (default) or PERCENTAGE.
     * If omitted, defaults to FIXED for backwards compatibility.
     */
    private AmountType amountType;

    /**
     * Required when amountType = PERCENTAGE.
     * Value: 0.01 to 100.00 (e.g. 10.00 = 10% of account balance).
     */
    @DecimalMin(value = "0.01", message = "Percentage must be at least 0.01")
    private BigDecimal percentageValue;

    /**
     * Optional. Skip execution if account balance < this threshold.
     * Applies before Phase 2C's basic balance check.
     */
    @DecimalMin(value = "0.00", inclusive = false, message = "Minimum balance threshold must be positive")
    private BigDecimal minimumBalanceThreshold;

    /**
     * If true, nextRunDate is pushed to Monday when it falls on Saturday or Sunday.
     * Defaults to false.
     */
    private Boolean skipWeekends;
}
