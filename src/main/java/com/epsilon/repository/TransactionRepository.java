package com.epsilon.repository;

import com.epsilon.entity.Account;
import com.epsilon.entity.Category;
import com.epsilon.entity.Transaction;
import com.epsilon.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Transaction entity.
 * This is the most critical repository - handles all financial transactions.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find all transactions for a specific account (as sender or receiver).
     * 
     * Generated SQL: SELECT * FROM transactions 
     *                WHERE from_account_id = ? OR to_account_id = ?
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount = :account OR t.toAccount = :account")
    List<Transaction> findByAccount(@Param("account") Account account);
    
    /**
     * Find transactions by account ID with pagination.
     * 
     * Pagination prevents loading 100,000 transactions into memory at once.
     * Returns "pages" of results (e.g., 20 transactions per page).
     * 
     * @param accountId The account ID
     * @param pageable Pagination parameters (page number, size, sort)
     * @return Page of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
    
    /**
     * Find transactions by type for a specific account.
     * 
     * Example use: Show all INCOME transactions
     */
    List<Transaction> findByFromAccountIdAndTransactionType(Long accountId, TransactionType type);
    
    /**
     * Find transactions by category.
     * 
     * Example use: Show all "Food" expenses
     */
    List<Transaction> findByCategory(Category category);
    
    /**
     * Find transactions by category ID with pagination.
     */
    Page<Transaction> findByCategoryId(Long categoryId, Pageable pageable);
    
    /**
     * Find transactions within a date range.
     * 
     * Critical for: "Show me transactions from last month"
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find transactions for a user within a date range.
     * 
     * Joins through accounts to filter by user ownership.
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE (t.fromAccount.user.id = :userId OR t.toAccount.user.id = :userId) " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Calculate total spent by category in a date range.
     * 
     * Critical for analytics: "How much did I spend on Food this month?"
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.category.id = :categoryId " +
           "AND t.transactionType = 'EXPENSE' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumExpensesByCategoryAndDateRange(
        @Param("categoryId") Long categoryId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Get recent transactions for a user (for dashboard display).
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.fromAccount.user.id = :userId OR t.toAccount.user.id = :userId " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
