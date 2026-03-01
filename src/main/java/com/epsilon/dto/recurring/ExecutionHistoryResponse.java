package com.epsilon.dto.recurring;

import com.epsilon.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a single execution record from the audit trail.
 *
 * Returned by:
 *   GET /api/recurring-transactions/{id}/history
 *   GET /api/recurring-transactions/{id}/stats
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionHistoryResponse {

    private Long id;
    private Long recurringTransactionId;

    /** Description snapshot from the recurring rule at time of mapping. */
    private String recurringTransactionDescription;

    /**
     * ID of the Module 1 Transaction created by this execution.
     * Null if status = FAILED.
     */
    private Long transactionId;

    private LocalDateTime executionDate;
    private ExecutionStatus status;

    /** Error details if FAILED. Null if SUCCESS. */
    private String errorMessage;

    /**
     * Amount snapshot at time of execution.
     * Preserved so history is accurate even if the rule amount is later edited.
     */
    private BigDecimal processedAmount;

    private LocalDateTime createdAt;
}
