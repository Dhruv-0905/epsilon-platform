package com.epsilon.repository;

import com.epsilon.entity.Account;
import com.epsilon.entity.User;
import com.epsilon.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Account entity operations.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * Find all accounts belonging to a specific user.
     * 
     * Generated SQL: SELECT * FROM accounts WHERE user_id = ?
     */
    List<Account> findByUser(User user);
    
    /**
     * Alternative: Find accounts by user ID directly.
     * More efficient when you only have the ID, not the full User object.
     * 
     * Generated SQL: SELECT * FROM accounts WHERE user_id = ?
     */
    List<Account> findByUserId(Long userId);
    
    /**
     * Find active accounts for a user.
     * 
     * Generated SQL: SELECT * FROM accounts WHERE user_id = ? AND is_active = TRUE
     */
    List<Account> findByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Find account by account number.
     * 
     * Generated SQL: SELECT * FROM accounts WHERE account_number = ?
     */
    Optional<Account> findByAccountNumber(String accountNumber);
    
    /**
     * Find all accounts of a specific type for a user.
     * 
     * Generated SQL: SELECT * FROM accounts WHERE user_id = ? AND account_type = ?
     */
    List<Account> findByUserIdAndAccountType(Long userId, AccountType accountType);
    
    /**
     * Custom JPQL query to calculate total balance across all user accounts.
     * 
     * JPQL (Java Persistence Query Language) is like SQL but uses entity/field names 
     * instead of table/column names.
     * 
     * @param userId The user ID
     * @return Total balance across all accounts (returns 0 if no accounts)
     */
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user.id = :userId AND a.isActive = true")
    java.math.BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);
    
    /**
     * Check if account number already exists (for validation before creating new accounts).
     */
    boolean existsByAccountNumber(String accountNumber);
}
