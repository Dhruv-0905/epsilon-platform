package com.epsilon.entity;

import com.epsilon.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit record for every execution (attempted or successful) of a RecurringTransaction rule.
 *
 * One row is written per scheduler run per rule, regardless of outcome.
 * This gives a full timeline of:
 *   - When each rule fired
 *   - Whether it succeeded or failed
 *   - Which Transaction was generated (if SUCCESS)
 *   - What error occurred (if FAILED)
 *
 * @author Epsilon Platform
 * @version 2.0 (Module 2B - Audit Trail)
 */
@Entity
@Table(name = "recurring_transaction_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The recurring rule that triggered this execution.
     * Never null - every execution belongs to exactly one rule.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_transaction_id", nullable = false)
    private RecurringTransaction recurringTransaction;

    /**
     * The Module 1 Transaction created by this execution.
     * Null if the execution FAILED (no transaction was persisted).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    /** Timestamp when the scheduler attempted this execution. */
    @Column(name = "execution_date", nullable = false)
    private LocalDateTime executionDate;

    /**
     * Outcome of this execution attempt.
     * SUCCESS = transaction created and balance updated.
     * FAILED  = exception thrown; see errorMessage for details.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status;

    /**
     * Error message captured when status = FAILED.
     * Truncated to 500 characters to fit the column.
     * Null when status = SUCCESS.
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * Amount snapshot at time of execution.
     * Preserved so history is accurate even if rule amount is later updated.
     */
    @Column(name = "processed_amount", precision = 15, scale = 2)
    private BigDecimal processedAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
