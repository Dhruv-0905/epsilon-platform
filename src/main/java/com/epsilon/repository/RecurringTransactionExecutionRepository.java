package com.epsilon.repository;

import com.epsilon.entity.RecurringTransactionExecution;
import com.epsilon.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for RecurringTransactionExecution audit records.
 *
 * Provides query methods for:
 * - Full history of a single recurring rule
 * - Filtered history by status (SUCCESS / FAILED)
 * - All executions across all rules for a user
 * - Counts for stats/dashboard display
 */
@Repository
public interface RecurringTransactionExecutionRepository
        extends JpaRepository<RecurringTransactionExecution, Long> {

    /**
     * Get all executions for a specific recurring rule, newest first.
     */
    List<RecurringTransactionExecution> findByRecurringTransactionIdOrderByExecutionDateDesc(
            Long recurringTransactionId);

    /**
     * Get paginated executions for a specific recurring rule, newest first.
     */
    Page<RecurringTransactionExecution> findByRecurringTransactionIdOrderByExecutionDateDesc(
            Long recurringTransactionId, Pageable pageable);

    /**
     * Get executions filtered by outcome status for a specific rule.
     * Example: find all FAILED executions to review errors.
     */
    List<RecurringTransactionExecution> findByRecurringTransactionIdAndStatusOrderByExecutionDateDesc(
            Long recurringTransactionId, ExecutionStatus status);

    /**
     * Get all executions across ALL recurring rules owned by a user, newest first.
     * Paginated to prevent loading thousands of records.
     */
    @Query("SELECT e FROM RecurringTransactionExecution e " +
           "WHERE e.recurringTransaction.user.id = :userId " +
           "ORDER BY e.executionDate DESC")
    Page<RecurringTransactionExecution> findByUserIdOrderByExecutionDateDesc(
            @Param("userId") Long userId, Pageable pageable);

    /** Count executions by status for a specific rule. Used for stats. */
    long countByRecurringTransactionIdAndStatus(Long recurringTransactionId, ExecutionStatus status);

    /** Count total executions for a specific rule. */
    long countByRecurringTransactionId(Long recurringTransactionId);
}
