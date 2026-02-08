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

    public List<RecurringTransaction> getRecurringTransactionsByUserId(Long userId) {
        log.debug("Fetching recurring transactions for user ID: {}", userId);
        return recurringRepository.findByUserId(userId);
    }

    public List<RecurringTransaction> getActiveRecurringTransactionsByUserId(Long userId) {
        log.debug("Fetching active recurring transactions for user ID: {}", userId);
        return recurringRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public Optional<RecurringTransaction> getRecurringTransactionById(Long id) {
        return recurringRepository.findById(id);
    }

    /**
     * Called by a scheduled job (to be added later) to process all due rules.
     */
    @Transactional
    public void processDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        log.info("Processing recurring transactions for date: {}", today);

        List<RecurringTransaction> due = recurringRepository.findDueRecurringTransactions(today);
        log.info("Found {} due recurring transactions", due.size());

        for (RecurringTransaction recurring : due) {
            try {
                processRecurringTransaction(recurring);
            } catch (Exception e) {
                log.error("Failed to process recurring transaction ID: {} - {}",
                          recurring.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed processing recurring transactions");
    }

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
