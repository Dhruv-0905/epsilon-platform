package com.epsilon.service;

import com.epsilon.entity.Account;
import com.epsilon.entity.RecurringTransaction;
import com.epsilon.entity.RecurringTransactionExecution;
import com.epsilon.entity.Transaction;
import com.epsilon.entity.User;
import com.epsilon.enums.ExecutionStatus;
import com.epsilon.repository.AccountRepository;
import com.epsilon.repository.RecurringTransactionExecutionRepository;
import com.epsilon.repository.RecurringTransactionRepository;
import com.epsilon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer for managing recurring transaction rules
 * and triggering actual Transactions from those rules.
 *
 * This service handles:
 * - Creating and managing recurring transaction rules
 * - Processing due recurring transactions automatically
 * - Generating actual Transaction records from recurring rules
 * - Recording every execution in the audit trail (Phase 2B)
 *
 * @author Epsilon Platform
 * @version 2.0 (Module 2A + 2B - The Vault)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final RecurringTransactionExecutionRepository executionRepository; // Phase 2B
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
        log.info("✓ Recurring transaction created successfully with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Get all recurring transactions for a user (both active and inactive).
     */
    public List<RecurringTransaction> getRecurringTransactionsByUserId(Long userId) {
        log.debug("Fetching recurring transactions for user ID: {}", userId);
        return recurringRepository.findByUserId(userId);
    }

    /**
     * Get only active recurring transactions for a user.
     */
    public List<RecurringTransaction> getActiveRecurringTransactionsByUserId(Long userId) {
        log.debug("Fetching active recurring transactions for user ID: {}", userId);
        return recurringRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Get a specific recurring transaction by ID.
     */
    public Optional<RecurringTransaction> getRecurringTransactionById(Long id) {
        return recurringRepository.findById(id);
    }

    /**
     * Process all due recurring transactions.
     *
     * Called by:
     * - Scheduled job (daily at 2 AM IST)
     * - Manual trigger endpoint (for testing/recovery)
     *
     * For each due rule:
     * 1. Creates a new Transaction in Module 1
     * 2. Updates nextRunDate to next occurrence
     * 3. Deactivates the rule if past end date
     * 4. Saves an execution audit record (SUCCESS or FAILED) — Phase 2B
     *
     * @return Number of successfully processed recurring transactions
     */
    @Transactional
    public int processDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Processing Recurring Transactions for: {}", today);
        log.info("═══════════════════════════════════════════════════════════");

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
                // Process and capture the created Transaction
                Transaction created = processRecurringTransaction(recurring);
                successCount++;

                // Phase 2B: record SUCCESS
                saveExecutionRecord(recurring, created, ExecutionStatus.SUCCESS, null);

                log.info("✓ Processed recurring ID: {} - {} [{}]",
                         recurring.getId(), recurring.getDescription(), recurring.getAmount());

            } catch (Exception e) {
                failureCount++;

                // Phase 2B: record FAILED with error details
                saveExecutionRecord(recurring, null, ExecutionStatus.FAILED, e.getMessage());

                log.error("✗ Failed recurring ID: {} - {} - Error: {}",
                          recurring.getId(), recurring.getDescription(), e.getMessage(), e);
            }
        }

        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Processing Complete  |  Success: {}  |  Failed: {}  |  Total: {}",
                 successCount, failureCount, due.size());
        log.info("═══════════════════════════════════════════════════════════");

        return successCount;
    }

    /**
     * Process a single recurring transaction.
     *
     * Creates a new Transaction and updates the recurring rule's nextRunDate.
     * Deactivates the rule if it has passed its end date.
     *
     * @param recurring The recurring transaction to process
     * @return The Transaction entity that was created — used by audit trail
     */
    private Transaction processRecurringTransaction(RecurringTransaction recurring) {
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
                throw new IllegalArgumentException(
                    "Unsupported transaction type for recurring: " + recurring.getTransactionType());
        }

        // Create the actual transaction (Module 1 integration)
        Transaction created = transactionService.createTransaction(transaction);

        // Advance the next run date
        LocalDate nextRun = recurring.getFrequency().getNextDate(recurring.getNextRunDate());
        recurring.setNextRunDate(nextRun);

        // Deactivate if past end date
        if (recurring.getEndDate() != null && nextRun.isAfter(recurring.getEndDate())) {
            recurring.setIsActive(Boolean.FALSE);
            log.info("Recurring transaction ID: {} deactivated (past end date: {})",
                     recurring.getId(), recurring.getEndDate());
        }

        recurringRepository.save(recurring);

        // Return created transaction for audit record linkage
        return created;
    }

    /**
     * Save an execution audit record.
     *
     * Called after every processing attempt (success or failure).
     * Errors during save are caught and logged — audit failure must never
     * break the main processing flow.
     *
     * @param recurring     The rule that was processed
     * @param transaction   The Transaction created (null if FAILED)
     * @param status        SUCCESS or FAILED
     * @param errorMessage  Error details (null if SUCCESS)
     */
    private void saveExecutionRecord(RecurringTransaction recurring,
                                     Transaction transaction,
                                     ExecutionStatus status,
                                     String errorMessage) {
        try {
            RecurringTransactionExecution execution = new RecurringTransactionExecution();
            execution.setRecurringTransaction(recurring);
            execution.setTransaction(transaction);
            execution.setExecutionDate(LocalDateTime.now());
            execution.setStatus(status);
            execution.setProcessedAmount(recurring.getAmount());

            // Truncate error message to 500-char column limit
            if (errorMessage != null) {
                execution.setErrorMessage(
                    errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage
                );
            }

            executionRepository.save(execution);
            log.debug("Saved {} execution record for recurring ID: {}", status, recurring.getId());
        } catch (Exception e) {
            log.error("Failed to save execution record for recurring ID: {} - {}",
                      recurring.getId(), e.getMessage());
        }
    }

    /**
     * Get full execution history for a specific recurring transaction rule.
     * Results are ordered by date descending (most recent first).
     *
     * @param recurringId The recurring transaction rule ID
     * @return List of all execution records
     */
    public List<RecurringTransactionExecution> getExecutionHistory(Long recurringId) {
        log.debug("Fetching execution history for recurring ID: {}", recurringId);
        return executionRepository.findByRecurringTransactionIdOrderByExecutionDateDesc(recurringId);
    }

    /**
     * Get execution statistics for a specific recurring transaction rule.
     *
     * Returns:
     * - totalExecutions
     * - successCount
     * - failedCount
     * - successRate (e.g. "100.0%")
     *
     * @param recurringId The recurring transaction rule ID
     * @return Map containing execution statistics
     */
    public Map<String, Object> getExecutionStats(Long recurringId) {
        log.debug("Fetching execution stats for recurring ID: {}", recurringId);

        long total = executionRepository.countByRecurringTransactionId(recurringId);
        long success = executionRepository.countByRecurringTransactionIdAndStatus(
                recurringId, ExecutionStatus.SUCCESS);
        long failed = executionRepository.countByRecurringTransactionIdAndStatus(
                recurringId, ExecutionStatus.FAILED);

        double successRate = total > 0 ? (double) success / total * 100.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("recurringTransactionId", recurringId);
        stats.put("totalExecutions", total);
        stats.put("successCount", success);
        stats.put("failedCount", failed);
        stats.put("successRate", String.format("%.1f%%", successRate));

        return stats;
    }

    /**
     * Update an existing recurring transaction rule.
     *
     * Only modifiable fields: description, amount, frequency, endDate, category.
     * startDate and nextRunDate are immutable to maintain audit integrity.
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

        RecurringTransaction saved = recurringRepository.save(existing);
        log.info("✓ Recurring transaction updated successfully: {}", id);
        return saved;
    }

    /**
     * Deactivate a recurring transaction rule.
     *
     * Does not delete the rule — it remains in history for audit purposes.
     */
    @Transactional
    public void deactivateRecurringTransaction(Long id) {
        log.info("Deactivating recurring transaction with ID: {}", id);

        RecurringTransaction recurring = recurringRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));

        recurring.setIsActive(Boolean.FALSE);
        recurringRepository.save(recurring);

        log.info("✓ Recurring transaction deactivated successfully: {}", id);
    }
}
