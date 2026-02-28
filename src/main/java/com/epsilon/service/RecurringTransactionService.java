package com.epsilon.service;

import com.epsilon.entity.Account;
import com.epsilon.entity.RecurringTransaction;
import com.epsilon.entity.Transaction;
import com.epsilon.entity.User;
import com.epsilon.repository.AccountRepository;
import com.epsilon.repository.RecurringTransactionRepository;
import com.epsilon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing recurring transaction rules
 * and triggering actual Transactions from those rules.
 * 
 * This service handles:
 * - Creating and managing recurring transaction rules
 * - Processing due recurring transactions automatically
 * - Generating actual Transaction records from recurring rules
 * 
 * @author Epsilon Platform
 * @version 2.0 (Module 2A - The Vault)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    /**
     * Create a new recurring transaction rule.
     * 
     * Validates:
     * - User exists
     * - Account exists and belongs to user
     * - Start date is not in the past (auto-corrects to today)
     * - End date is after start date
     * 
     * @param userId ID of the user creating the recurring rule
     * @param recurring The recurring transaction rule to create
     * @return The saved recurring transaction with generated ID and nextRunDate
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public RecurringTransaction createRecurringTransaction(Long userId, RecurringTransaction recurring) {
        log.info("Creating recurring transaction for user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Account account = accountRepository.findById(recurring.getAccount().getId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to this user");
        }

        LocalDate today = LocalDate.now();
        if (recurring.getStartDate().isBefore(today)) {
            log.warn("Start date {} is in the past, adjusting to today", recurring.getStartDate());
            recurring.setStartDate(today);
        }

        if (recurring.getEndDate() != null &&
            recurring.getEndDate().isBefore(recurring.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        recurring.setNextRunDate(recurring.getStartDate());
        recurring.setUser(user);
        recurring.setAccount(account);

        if (recurring.getIsActive() == null) {
            recurring.setIsActive(true);
        }

        RecurringTransaction saved = recurringRepository.save(recurring);
        log.info("Recurring transaction created successfully with ID: {}", saved.getId());
        return saved;
    }
    /**
     * Get all recurring transactions for a user (both active and inactive).
     * 
     * @param userId The user ID
     * @return List of all recurring transactions
     */
    public List<RecurringTransaction> getRecurringTransactionsByUserId(Long userId) {
        log.debug("Fetching recurring transactions for user ID: {}", userId);
        return recurringRepository.findByUserId(userId);
    }
        /**
     * Get only active recurring transactions for a user.
     * 
     * @param userId The user ID
     * @return List of active recurring transactions
     */
    public List<RecurringTransaction> getActiveRecurringTransactionsByUserId(Long userId) {
        log.debug("Fetching active recurring transactions for user ID: {}", userId);
        return recurringRepository.findByUserIdAndIsActiveTrue(userId);
    }
        /**
     * Get a specific recurring transaction by ID.
     * 
     * @param id The recurring transaction ID
     * @return Optional containing the recurring transaction if found
     */
    public Optional<RecurringTransaction> getRecurringTransactionById(Long id) {
        return recurringRepository.findById(id);
    }

    /**
     * Get a specific recurring transaction by ID.
     * 
     * @param id The recurring transaction ID
     * @return Optional containing the recurring transaction if found
     */
    public Optional<RecurringTransaction> getRecurringTransactionById(Long id) {
        return recurringRepository.findById(id);
    }

    /**
     * Process all due recurring transactions.
     * 
     * This method is called by:
     * - Scheduled job (daily at 2 AM IST)
     * - Manual trigger endpoint (for testing/recovery)
     * 
     * For each due recurring rule:
     * 1. Creates a new Transaction in Module 1
     * 2. Updates the nextRunDate to the next occurrence
     * 3. Deactivates the rule if past end date
     * 
     * Errors are logged but don't stop processing of other rules.
     * 
     * @return Number of successfully processed recurring transactions
     */
    @Transactional
    public int processDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        log.info("┌─────────────────────────────────────────────────────────┐");
        log.info("│ Processing Recurring Transactions for: {}         │", today);
        log.info("└─────────────────────────────────────────────────────────┘");

        List<RecurringTransaction> due = recurringRepository.findDueRecurringTransactions(today);
        log.info("Found {} due recurring transaction(s)", due.size());

        if (due.isEmpty()) {
            log.info("No recurring transactions due for processing today");
            return 0;
        }

        int successCount = 0;
        int failureCount = 0;

        for (RecurringTransaction recurring : due) {
            try {
                processRecurringTransaction(recurring);
                successCount++;
                log.info("✓ Processed recurring ID: {} - {} [{}]", 
                         recurring.getId(), 
                         recurring.getDescription(),
                         recurring.getAmount());
            } catch (Exception e) {
                failureCount++;
                log.error("✗ Failed recurring ID: {} - {} - Error: {}", 
                          recurring.getId(), 
                          recurring.getDescription(),
                          e.getMessage(), e);
            }
        }

        log.info("┌─────────────────────────────────────────────────────────┐");
        log.info("│ Processing Complete                                     │");
        log.info("│ Success: {} | Failed: {} | Total: {}                  │", 
                 successCount, failureCount, due.size());
        log.info("└─────────────────────────────────────────────────────────┘");

        return successCount;
    }
    /**
     * Process a single recurring transaction.
     * 
     * Creates a new Transaction and updates the recurring rule's nextRunDate.
     * If the rule has passed its end date, it will be deactivated.
     * 
     * @param recurring The recurring transaction to process
     */
    private void processRecurringTransaction(RecurringTransaction recurring) {
        log.debug("Processing recurring transaction ID: {}", recurring.getId());

        Transaction transaction = new Transaction();
        transaction.setAmount(recurring.getAmount());
        transaction.setCurrency(recurring.getCurrency());
        transaction.setTransactionType(recurring.getTransactionType());
        transaction.setDescription(recurring.getDescription() + " (Recurring)");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCategory(recurring.getCategory());

        switch (recurring.getTransactionType()) {
            case INCOME:
                transaction.setToAccount(recurring.getAccount());
                break;
            case EXPENSE:
                transaction.setFromAccount(recurring.getAccount());
                break;
            default:
                log.error("Unsupported transaction type for recurring: {}",
                          recurring.getTransactionType());
                return;
        }

        transactionService.createTransaction(transaction);

        LocalDate nextRun = recurring.getFrequency().getNextDate(recurring.getNextRunDate());
        recurring.setNextRunDate(nextRun);

        if (recurring.getEndDate() != null &&
            nextRun.isAfter(recurring.getEndDate())) {
            recurring.setIsActive(false);
            log.info("Recurring transaction ID: {} deactivated (past end date)",
                     recurring.getId());
        }

        recurringRepository.save(recurring);
    }
    /**
     * Update an existing recurring transaction rule.
     * 
     * Only modifiable fields:
     * - description
     * - amount
     * - frequency
     * - endDate
     * - category
     * 
     * Start date and next run date are not modifiable to maintain audit trail.
     * 
     * @param id The ID of the recurring transaction to update
     * @param updated The updated recurring transaction data
     * @return The updated recurring transaction
     * @throws IllegalArgumentException if recurring transaction not found
     */
    @Transactional
    public RecurringTransaction updateRecurringTransaction(Long id, RecurringTransaction updated) {
        log.info("Updating recurring transaction with ID: {}", id);

        RecurringTransaction existing = recurringRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));

        existing.setDescription(updated.getDescription());
        existing.setAmount(updated.getAmount());
        existing.setFrequency(updated.getFrequency());
        existing.setEndDate(updated.getEndDate());
        existing.setCategory(updated.getCategory());

        return recurringRepository.save(existing);
    }

    @Transactional
    public void deactivateRecurringTransaction(Long id) {
        log.info("Deactivating recurring transaction with ID: {}", id);

        RecurringTransaction recurring = recurringRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));

        recurring.setIsActive(false);
        recurringRepository.save(recurring);

        log.info("Recurring transaction deactivated successfully: {}", id);
    }
}
