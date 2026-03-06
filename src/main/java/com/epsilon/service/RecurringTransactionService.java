package com.epsilon.service;

import com.epsilon.entity.Account;
import com.epsilon.entity.Category;
import com.epsilon.entity.RecurringTransaction;
import com.epsilon.entity.RecurringTransactionExecution;
import com.epsilon.entity.Transaction;
import com.epsilon.entity.User;
import com.epsilon.enums.AmountType;
import com.epsilon.enums.ExecutionStatus;
import com.epsilon.enums.RecurringFrequency;
import com.epsilon.enums.TransactionType;
import com.epsilon.repository.AccountRepository;
import com.epsilon.repository.CategoryRepository;
import com.epsilon.repository.RecurringTransactionExecutionRepository;
import com.epsilon.repository.RecurringTransactionRepository;
import com.epsilon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * - Pause/Resume of rules (Phase 2C)
 * - Insufficient balance detection and SKIPPED execution recording (Phase 2C)
 * - Pre-defined templates for quick rule creation (Phase 2C)
 *
 * @author Epsilon Platform
 * @version 2.1 (Module 2C - Smart Features)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final RecurringTransactionExecutionRepository executionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────────────────

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
        recurring.setIsActive(true);
        recurring.setIsPaused(false);
        recurring.setAmountType(recurring.getAmountType() != null ? recurring.getAmountType() : AmountType.FIXED);
        recurring.setSkipWeekends(Boolean.TRUE.equals(recurring.getSkipWeekends()));

        RecurringTransaction saved = recurringRepository.save(recurring);
        log.info("✓ Recurring transaction created with ID: {}", saved.getId());
        return saved;
    }

    public List<RecurringTransaction> getRecurringTransactionsByUserId(Long userId) {
        return recurringRepository.findByUserId(userId);
    }

    public List<RecurringTransaction> getActiveRecurringTransactionsByUserId(Long userId) {
        return recurringRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public Optional<RecurringTransaction> getRecurringTransactionById(Long id) {
        return recurringRepository.findById(id);
    }

    /**
     * Update allowed fields of a recurring transaction rule.
     * startDate and nextRunDate are immutable to preserve audit integrity.
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
        log.info("✓ Recurring transaction updated: {}", id);
        return saved;
    }

    /**
     * Permanently deactivate a recurring transaction rule.
     * Rule remains in history. Cannot be reactivated via normal flow.
     */
    @Transactional
    public void deactivateRecurringTransaction(Long id) {
        log.info("Deactivating recurring transaction with ID: {}", id);

        RecurringTransaction recurring = recurringRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));

        recurring.setIsActive(false);
        recurringRepository.save(recurring);

        log.info("✓ Recurring transaction deactivated: {}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2C: Pause / Resume
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Temporarily pause a recurring transaction rule.
     *
     * While paused:
     * - The scheduler skips this rule entirely
     * - nextRunDate does NOT advance (rule catches up on resume)
     * - Execution records are NOT written (rule is invisible to scheduler)
     *
     * Differs from deactivation: pause is reversible.
     *
     * @param id     The recurring transaction ID to pause
     * @param reason Optional human-readable reason (e.g. "On vacation")
     * @throws IllegalArgumentException if rule not found or already paused/inactive
     */
    @Transactional
    public RecurringTransaction pauseRecurringTransaction(Long id, String reason) {
        log.info("Pausing recurring transaction ID: {}", id);

        RecurringTransaction recurring = recurringRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));

        // Null-safe: treats null as false (inactive)
        if (!Boolean.TRUE.equals(recurring.getIsActive())) {
            throw new IllegalArgumentException("Cannot pause an inactive recurring transaction");
        }

        if (Boolean.TRUE.equals(recurring.getIsPaused())) {
            throw new IllegalArgumentException("Recurring transaction is already paused");
        }

        recurring.setIsPaused(true);
        recurring.setPauseReason(reason != null ? reason : "Paused by user");

        RecurringTransaction saved = recurringRepository.save(recurring);
        log.info("✓ Recurring transaction paused: {} — reason: {}", id, recurring.getPauseReason());
        return saved;
    }
    
    /**
     * Resume a previously paused recurring transaction rule.
     *
     * On resume:
     * - isPaused is set to false
     * - pauseReason is cleared
     * - nextRunDate is reset to today if it has fallen behind
     *   (so the rule fires on the next scheduler run rather than
     *    creating a flood of catch-up transactions)
     *
     * @param id The recurring transaction ID to resume
     * @throws IllegalArgumentException if rule not found or not currently paused
     */
    @Transactional
    public RecurringTransaction resumeRecurringTransaction(Long id) {
        log.info("Resuming recurring transaction ID: {}", id);

        RecurringTransaction recurring = recurringRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));

        if (!Boolean.TRUE.equals(recurring.getIsPaused())) {
            throw new IllegalArgumentException("Recurring transaction is not currently paused");
        }

        recurring.setIsPaused(false);
        recurring.setPauseReason(null);

        // Reset nextRunDate to today if it has fallen behind during the pause
        // This prevents a flood of back-dated catch-up transactions
        LocalDate today = LocalDate.now();
        if (recurring.getNextRunDate().isBefore(today)) {
            log.info("Adjusting nextRunDate from {} to today ({}) after resume",
                     recurring.getNextRunDate(), today);
            recurring.setNextRunDate(today);
        }

        RecurringTransaction saved = recurringRepository.save(recurring);
        log.info("✓ Recurring transaction resumed: {} — next run: {}", id, saved.getNextRunDate());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scheduler Processing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Process all due recurring transactions.
     *
     * Called by:
     * - Scheduled job (daily at 2 AM IST)
     * - Manual trigger endpoint (for testing/recovery)
     *
     * For each due rule:
     * 1. Checks balance if EXPENSE (Phase 2C) — records SKIPPED if insufficient
     * 2. Creates a new Transaction in Module 1
     * 3. Updates nextRunDate to next occurrence
     * 4. Deactivates the rule if past end date
     * 5. Saves execution audit record (SUCCESS / FAILED / SKIPPED) — Phase 2B + 2C
     *
     * Paused rules are excluded by the repository query — they never reach this method.
     *
     * @return Number of successfully processed recurring transactions
     */
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
        int skippedCount = 0;
        int failureCount = 0;

        for (RecurringTransaction recurring : due) {
            try {
                Transaction created = processRecurringTransaction(recurring);

                if (created == null) {
                    // null = explicitly skipped (insufficient balance)
                    skippedCount++;
                    saveExecutionRecord(recurring, null, ExecutionStatus.SKIPPED,
                        "Insufficient account balance — transaction skipped");
                    log.warn("⊘ Skipped recurring ID: {} - {} — insufficient balance",
                             recurring.getId(), recurring.getDescription());
                } else {
                    successCount++;
                    saveExecutionRecord(recurring, created, ExecutionStatus.SUCCESS, null);
                    log.info("✓ Processed recurring ID: {} - {} [{}]",
                             recurring.getId(), recurring.getDescription(), recurring.getAmount());
                }

            } catch (Exception e) {
                failureCount++;
                saveExecutionRecord(recurring, null, ExecutionStatus.FAILED, e.getMessage());
                log.error("✗ Failed recurring ID: {} - {} — Error: {}",
                          recurring.getId(), recurring.getDescription(), e.getMessage(), e);
            }
        }

        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Done | Success: {} | Skipped: {} | Failed: {} | Total: {}",
                 successCount, skippedCount, failureCount, due.size());
        log.info("═══════════════════════════════════════════════════════════");

        return successCount;
    }

    /**
     * Process a single recurring transaction.
     *
     * Phase 2C: Balance check for EXPENSE transactions.
     * Phase 2D: Variable amount calculation, minimum balance threshold, weekend skip.
     *
     * @param recurring The recurring transaction to process
     * @return The created Transaction entity, or null if SKIPPED
     */
    private Transaction processRecurringTransaction(RecurringTransaction recurring) {
        log.debug("Processing recurring transaction ID: {}", recurring.getId());

        // Fetch fresh account balance for all checks
        Account freshAccount = accountRepository.findById(recurring.getAccount().getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Account not found: " + recurring.getAccount().getId()));

        // Phase 2D: Minimum balance threshold check (user-defined safety floor)
        if (recurring.getMinimumBalanceThreshold() != null) {
            if (freshAccount.getBalance().compareTo(recurring.getMinimumBalanceThreshold()) < 0) {
                log.warn("Minimum balance threshold not met for recurring ID: {}. " +
                        "Balance: {}, Threshold: {}",
                        recurring.getId(), freshAccount.getBalance(),
                        recurring.getMinimumBalanceThreshold());
                return null; // Signals SKIPPED
            }
        }

        // Phase 2D: Compute actual execution amount
        BigDecimal executionAmount = computeExecutionAmount(recurring, freshAccount);
        log.debug("Computed execution amount for recurring ID: {} = {}", recurring.getId(), executionAmount);

        // Phase 2C: Insufficient balance check for EXPENSE (uses computed amount)
        if (TransactionType.EXPENSE.equals(recurring.getTransactionType())) {
            if (freshAccount.getBalance().compareTo(executionAmount) < 0) {
                log.warn("Insufficient balance for recurring ID: {}. Required: {}, Available: {}",
                        recurring.getId(), executionAmount, freshAccount.getBalance());
                return null; // Signals SKIPPED
            }
        }

        // Build the transaction from the recurring rule
        Transaction transaction = new Transaction();
        transaction.setAmount(executionAmount);
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

        Transaction created = transactionService.createTransaction(transaction);

        // Advance nextRunDate + Phase 2D: weekend skip
        LocalDate nextRun = recurring.getFrequency().getNextDate(recurring.getNextRunDate());
        nextRun = adjustForWeekends(nextRun, recurring);
        recurring.setNextRunDate(nextRun);

        // Deactivate if past end date
        if (recurring.getEndDate() != null && nextRun.isAfter(recurring.getEndDate())) {
            recurring.setIsActive(false);
            log.info("Recurring ID: {} deactivated — past end date: {}",
                    recurring.getId(), recurring.getEndDate());
        }

        recurringRepository.save(recurring);
        return created;
    }

    /**
     * Compute the actual amount to use for this execution.
     *
     * FIXED:      Returns recurring.getAmount() directly.
     * PERCENTAGE: Calculates balance * percentageValue / 100.
     *             Caps at recurring.getAmount() as a safety maximum.
     *
     * @param recurring    The rule being processed
     * @param freshAccount Account with the current live balance
     * @return The computed execution amount, never null or zero
     */
    private BigDecimal computeExecutionAmount(RecurringTransaction recurring, Account freshAccount) {
        AmountType type = recurring.getAmountType() != null ? recurring.getAmountType() : AmountType.FIXED;

        if (AmountType.PERCENTAGE.equals(type) && recurring.getPercentageValue() != null) {
            // computed = balance * (percentage / 100)
            BigDecimal computed = freshAccount.getBalance()
                .multiply(recurring.getPercentageValue())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            // Apply safety cap: never exceed the 'amount' field
            BigDecimal cap = recurring.getAmount();
            BigDecimal capped = computed.compareTo(cap) > 0 ? cap : computed;

            log.debug("PERCENTAGE amount: {}% of {} = {} (cap: {})",
                    recurring.getPercentageValue(), freshAccount.getBalance(), computed, cap);

            // Guard: never return zero or negative
            return capped.compareTo(BigDecimal.ZERO) > 0 ? capped : recurring.getAmount();
        }
        return recurring.getAmount();
    }

    /**
     * Adjust a date forward to the next Monday if it falls on a weekend.
     * Only applied when the rule has skipWeekends = true.
     *
     * Saturday → +2 days → Monday
     * Sunday   → +1 day  → Monday
     *
     * @param date      The candidate next run date
     * @param recurring The rule (checked for skipWeekends flag)
     * @return The adjusted date (unchanged if not a weekend or skipWeekends=false)
     */
    private LocalDate adjustForWeekends(LocalDate date, RecurringTransaction recurring) {
        if (!Boolean.TRUE.equals(recurring.getSkipWeekends())) {
            return date;
        }

        switch (date.getDayOfWeek()) {
            case SATURDAY:
                log.debug("Recurring ID: {} — nextRunDate {} is Saturday, advancing to Monday",
                        recurring.getId(), date);
                return date.plusDays(2);
            case SUNDAY:
                log.debug("Recurring ID: {} — nextRunDate {} is Sunday, advancing to Monday",
                        recurring.getId(), date);
                return date.plusDays(1);
            default:
                return date;
        }
    }

    /**
     * Save an execution audit record.
     * Errors during save are caught — audit failure must never affect main processing.
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

            if (errorMessage != null) {
                execution.setErrorMessage(
                    errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage
                );
            }

            executionRepository.save(execution);
        } catch (Exception e) {
            log.error("Failed to save execution record for recurring ID: {} — {}",
                      recurring.getId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2B: Execution History & Stats
    // ─────────────────────────────────────────────────────────────────────────

    public List<RecurringTransactionExecution> getExecutionHistory(Long recurringId) {
        return executionRepository.findByRecurringTransactionIdOrderByExecutionDateDesc(recurringId);
    }

    /**
     * Get execution statistics for a recurring rule.
     * Now includes skippedCount for Phase 2C.
     */
    public Map<String, Object> getExecutionStats(Long recurringId) {
        long total   = executionRepository.countByRecurringTransactionId(recurringId);
        long success = executionRepository.countByRecurringTransactionIdAndStatus(recurringId, ExecutionStatus.SUCCESS);
        long failed  = executionRepository.countByRecurringTransactionIdAndStatus(recurringId, ExecutionStatus.FAILED);
        long skipped = executionRepository.countByRecurringTransactionIdAndStatus(recurringId, ExecutionStatus.SKIPPED);

        double successRate = total > 0 ? (double) success / total * 100.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("recurringTransactionId", recurringId);
        stats.put("totalExecutions", total);
        stats.put("successCount", success);
        stats.put("failedCount", failed);
        stats.put("skippedCount", skipped);
        stats.put("successRate", String.format("%.1f%%", successRate));

        return stats;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2C: Templates
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get all pre-defined recurring transaction templates.
     *
     * Templates supply transactionType, frequency and a suggested description.
     * The user provides amount, currency, account and dates when creating from template.
     *
     * No database table needed — templates are static definitions.
     *
     * @return List of template maps for API response
     */
    public List<Map<String, Object>> getTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        templates.add(buildTemplate("MONTHLY_SALARY",       "Monthly Salary",       TransactionType.INCOME,   RecurringFrequency.MONTHLY, "Auto-credit your salary on the same day each month"));
        templates.add(buildTemplate("MONTHLY_RENT",         "Monthly Rent",         TransactionType.EXPENSE,  RecurringFrequency.MONTHLY, "Auto-debit rent payment to your landlord"));
        templates.add(buildTemplate("WEEKLY_GROCERIES",     "Weekly Groceries",     TransactionType.EXPENSE,  RecurringFrequency.WEEKLY,  "Auto-debit weekly grocery budget"));
        templates.add(buildTemplate("MONTHLY_UTILITIES",    "Monthly Utilities",    TransactionType.EXPENSE,  RecurringFrequency.MONTHLY, "Auto-debit electricity, water and gas bills"));
        templates.add(buildTemplate("MONTHLY_SUBSCRIPTION", "Monthly Subscription", TransactionType.EXPENSE,  RecurringFrequency.MONTHLY, "Auto-debit streaming or software subscription fees"));
        return templates;
    }

    private Map<String, Object> buildTemplate(String id, String name,
                                               TransactionType type,
                                               RecurringFrequency frequency,
                                               String description) {
        Map<String, Object> t = new HashMap<>();
        t.put("templateId", id);
        t.put("name", name);
        t.put("transactionType", type);
        t.put("frequency", frequency);
        t.put("suggestedDescription", description);
        return t;
    }

    /**
     * Create a recurring transaction rule from a pre-defined template.
     *
     * The template ID determines transactionType and frequency.
     * User provides amount, currency, accountId, startDate and optional endDate/categoryId.
     *
     * @param userId  The user creating the rule
     * @param templateId  One of the IDs returned by getTemplates()
     * @param accountId   Account to associate
     * @param amount      Transaction amount
     * @param currency    Transaction currency
     * @param startDate   Rule start date
     * @param endDate     Optional rule end date (null = runs forever)
     * @param categoryId  Optional category ID
     * @return The saved recurring transaction rule
     */
    @Transactional
    public RecurringTransaction createFromTemplate(Long userId,
                                                    String templateId,
                                                    Long accountId,
                                                    BigDecimal amount,
                                                    com.epsilon.enums.Currency currency,
                                                    LocalDate startDate,
                                                    LocalDate endDate,
                                                    Long categoryId) {
        log.info("Creating recurring transaction from template '{}' for user ID: {}", templateId, userId);

        // Resolve template
        Map<String, Object> template = getTemplates().stream()
            .filter(t -> templateId.equals(t.get("templateId")))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown template ID: " + templateId + ". Call GET /api/recurring-transactions/templates for valid IDs."));

        // Build recurring rule from template + user inputs
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setTransactionType((TransactionType) template.get("transactionType"));
        recurring.setFrequency((RecurringFrequency) template.get("frequency"));
        recurring.setDescription((String) template.get("suggestedDescription"));
        recurring.setAmount(amount);
        recurring.setCurrency(currency);
        recurring.setStartDate(startDate);
        recurring.setEndDate(endDate);

        // Set a placeholder account — createRecurringTransaction will validate & reload it
        Account accountRef = new Account();
        accountRef.setId(accountId);
        recurring.setAccount(accountRef);

        // Optionally set category
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
            recurring.setCategory(category);
        }

        return createRecurringTransaction(userId, recurring);
    }
}
