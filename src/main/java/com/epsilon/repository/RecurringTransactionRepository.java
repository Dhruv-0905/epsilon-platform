package com.epsilon.repository;

import com.epsilon.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for RecurringTransaction entity.
 * Used by the scheduled job to process recurring rules.
 */
@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    
    /**
     * Find all recurring transactions for a user.
     */
    List<RecurringTransaction> findByUserId(Long userId);
    
    /**
     * Find active recurring transactions for a user.
     */
    List<RecurringTransaction> findByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Find recurring transactions that are due for processing.
     * 
     * This is THE CRITICAL QUERY for the scheduled job.
     * 
     * Logic:
     * - isActive = true (not paused/deleted)
     * - nextRunDate <= today (due or overdue)
     * - endDate is null OR endDate >= today (not expired)
     * 
     * The scheduled job will call this every day at midnight.
     * 
     * @param today Current date
     * @return List of recurring transactions ready to process
     */
    @Query("SELECT r FROM RecurringTransaction r " +
           "WHERE r.isActive = true " +
           "AND r.nextRunDate <= :today " +
           "AND (r.endDate IS NULL OR r.endDate >= :today)")
    List<RecurringTransaction> findDueRecurringTransactions(@Param("today") LocalDate today);
    
    /**
     * Find recurring transactions by account.
     * 
     * Useful for: "Show me all auto-payments from my checking account"
     */
    List<RecurringTransaction> findByAccountId(Long accountId);
    
    /**
     * Find recurring transactions by account and active status.
     */
    List<RecurringTransaction> findByAccountIdAndIsActiveTrue(Long accountId);
}
