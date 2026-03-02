package com.epsilon.entity;

import com.epsilon.enums.AmountType;
import com.epsilon.enums.Currency;
import com.epsilon.enums.RecurringFrequency;
import com.epsilon.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a recurring transaction rule (e.g., monthly salary, weekly groceries).
 * A scheduled job processes these rules and auto-creates Transaction records.
 *
 * State model:
 *   isActive=true,  isPaused=false → ACTIVE   (normal processing)
 *   isActive=true,  isPaused=true  → PAUSED   (temporarily suspended)
 *   isActive=false, isPaused=false → INACTIVE  (permanently deactivated)
 *
 * Phase 2D additions:
 *   amountType            - FIXED or PERCENTAGE (default FIXED)
 *   percentageValue       - Used when amountType=PERCENTAGE (0-100)
 *   minimumBalanceThreshold - Skip execution if account balance < this value
 *   skipWeekends          - Advance nextRunDate to Monday if it falls on Sat/Sun
 */
@Entity
@Table(name = "recurring_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Description is required")
    @Column(name = "description", nullable = false)
    private String description;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Currency is required")
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Transaction type is required")
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Frequency is required")
    @Column(name = "frequency", nullable = false)
    private RecurringFrequency frequency;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;  // Null = runs forever

    @NotNull(message = "Next run date is required")
    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    /**
     * Whether this rule is permanently active.
     * false = permanently deactivated (cannot be reactivated via normal flow).
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Whether this rule is temporarily paused.
     * true  = skip during scheduler runs (nextRunDate does NOT advance).
     * false = process normally.
     *
     * Pause is reversible — resume restores normal processing.
     * Differs from isActive: a paused rule remains in history and can be resumed.
     */
    @Column(name = "is_paused")
    private Boolean isPaused = false;

    /**
     * Optional human-readable reason why this rule was paused
     * Shown to user in pause/resume responses.
     */
    @Column(name = "pause_reason", length = 255)
    private String pauseReason;

    // ── Phase 2D ──────────────────────────────────────────────────────────────

    /**
     * How the execution amount is calculated.
     * Defaults to FIXED for full backwards compatibility.
     * Existing rows get NULL → service treats NULL as FIXED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "amount_type")
    private AmountType amountType = AmountType.FIXED;

    /**
     * Percentage of account balance to use when amountType = PERCENTAGE.
     * Value range: 0.01 – 100.00 (e.g. 10.00 = 10%).
     * When amountType = FIXED this field is ignored.
     * 'amount' acts as a safety cap for PERCENTAGE calculations.
     */
    @Column(name = "percentage_value", precision = 5, scale = 2)
    private BigDecimal percentageValue;

    /**
     * Skip execution if account balance is below this threshold (Phase 2D).
     * Gives users fine-grained control independent of Phase 2C's basic check.
     *
     * Logic (applied before amount check):
     *   if minimumBalanceThreshold != null AND balance < threshold → SKIPPED
     *   else Phase 2C's standard balance >= computedAmount check applies.
     *
     * Example: "Never process my rent rule if my balance drops below $500"
     */
    @Column(name = "minimum_balance_threshold", precision = 15, scale = 2)
    private BigDecimal minimumBalanceThreshold;

    /**
     * If true, when nextRunDate falls on Saturday or Sunday it is
     * automatically pushed forward to the following Monday.
     *
     * Useful for salary rules where you want processing on a business day.
     */
    @Column(name = "skip_weekends")
    private Boolean skipWeekends = false;

    // ── Timestamps ────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Relationships ─────────────────────────────────────────────────────────
    @NotNull(message = "Account is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
