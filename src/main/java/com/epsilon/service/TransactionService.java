package com.epsilon.service;

import com.epsilon.entity.Account;
import com.epsilon.entity.Category;
import com.epsilon.entity.Transaction;
import com.epsilon.enums.TransactionType;
import com.epsilon.enums.Currency;
import com.epsilon.repository.AccountRepository;
import com.epsilon.repository.CategoryRepository;
import com.epsilon.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core money-movement service.
 * All balance changes must go through this service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Create a new transaction (INCOME, EXPENSE, TRANSFER).
     * All account updates + transaction insert are atomic.
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        log.info("Creating {} transaction of amount: {} {}",
                 transaction.getTransactionType(),
                 transaction.getAmount(),
                 transaction.getCurrency());

        if (transaction.getTransactionType() == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        switch (transaction.getTransactionType()) {
            case INCOME:
                processIncome(transaction);
                break;
            case EXPENSE:
                processExpense(transaction);
                break;
            case TRANSFER:
                processTransfer(transaction);
                break;
            default:
                throw new IllegalArgumentException("Invalid transaction type: " + transaction.getTransactionType());
        }

        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDateTime.now());
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction created successfully with ID: {}", savedTransaction.getId());
        return savedTransaction;
    }

    private void processIncome(Transaction transaction) {
        if (transaction.getToAccount() == null) {
            throw new IllegalArgumentException("INCOME transaction requires a destination account");
        }

        Account toAccount = accountRepository.findById(transaction.getToAccount().getId())
            .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        validateAccountActive(toAccount, "Destination");
        validateCurrencyMatch(transaction.getCurrency(), toAccount.getCurrency(), "destination");

        BigDecimal oldBalance = toAccount.getBalance();
        BigDecimal newBalance = oldBalance.add(transaction.getAmount());
        toAccount.setBalance(newBalance);
        accountRepository.save(toAccount);

        log.debug("INCOME: Account {} balance updated from {} to {}",
                  toAccount.getId(), oldBalance, newBalance);
    }

    private void processExpense(Transaction transaction) {
        if (transaction.getFromAccount() == null) {
            throw new IllegalArgumentException("EXPENSE transaction requires a source account");
        }

        Account fromAccount = accountRepository.findById(transaction.getFromAccount().getId())
            .orElseThrow(() -> new IllegalArgumentException("Source account not found"));

        validateAccountActive(fromAccount, "Source");
        validateCurrencyMatch(transaction.getCurrency(), fromAccount.getCurrency(), "source");

        if (!fromAccount.getAccountType().toString().contains("CREDIT")) {
            if (fromAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
                throw new IllegalArgumentException(
                    String.format("Insufficient balance. Available: %s, Required: %s",
                                  fromAccount.getBalance(), transaction.getAmount())
                );
            }
        }

        BigDecimal oldBalance = fromAccount.getBalance();
        BigDecimal newBalance = oldBalance.subtract(transaction.getAmount());
        fromAccount.setBalance(newBalance);
        accountRepository.save(fromAccount);

        log.debug("EXPENSE: Account {} balance updated from {} to {}",
                  fromAccount.getId(), oldBalance, newBalance);
    }

    private void processTransfer(Transaction transaction) {
        if (transaction.getFromAccount() == null || transaction.getToAccount() == null) {
            throw new IllegalArgumentException("TRANSFER requires both source and destination accounts");
        }

        if (transaction.getFromAccount().getId().equals(transaction.getToAccount().getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account fromAccount = accountRepository.findById(transaction.getFromAccount().getId())
            .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account toAccount = accountRepository.findById(transaction.getToAccount().getId())
            .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        validateAccountActive(fromAccount, "Source");
        validateAccountActive(toAccount, "Destination");
        validateCurrencyMatch(transaction.getCurrency(), fromAccount.getCurrency(), "source");
        validateCurrencyMatch(transaction.getCurrency(), toAccount.getCurrency(), "destination");

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: From account uses %s, To account uses %s. Conversion not supported yet.",
                              fromAccount.getCurrency(), toAccount.getCurrency())
            );
        }

        if (fromAccount.getBalance().compareTo(transaction.getAmount()) < 0) {
            throw new IllegalArgumentException(
                String.format("Insufficient balance in source account. Available: %s, Required: %s",
                              fromAccount.getBalance(), transaction.getAmount())
            );
        }

        BigDecimal fromOld = fromAccount.getBalance();
        BigDecimal toOld = toAccount.getBalance();

        BigDecimal fromNew = fromOld.subtract(transaction.getAmount());
        BigDecimal toNew = toOld.add(transaction.getAmount());

        fromAccount.setBalance(fromNew);
        toAccount.setBalance(toNew);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.debug("TRANSFER: {} moved from account {} ({} -> {}) to account {} ({} -> {})",
                  transaction.getAmount(),
                  fromAccount.getId(), fromOld, fromNew,
                  toAccount.getId(), toOld, toNew);
    }

    private void validateAccountActive(Account account, String label) {
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new IllegalArgumentException(label + " account is not active: " + account.getId());
        }
    }

    private void validateCurrencyMatch(Currency txnCurrency,
                                       Currency accountCurrency,
                                       String label) {
        if (txnCurrency != accountCurrency) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: Transaction uses %s, %s account uses %s",
                              txnCurrency, label, accountCurrency)
            );
        }
    }

    public Optional<Transaction> getTransactionById(Long transactionId) {
        log.debug("Fetching transaction with ID: {}", transactionId);
        return transactionRepository.findById(transactionId);
    }

    public Page<Transaction> getTransactionsByAccountId(Long accountId, Pageable pageable) {
        log.debug("Fetching transactions for account ID: {} (page: {})",
                  accountId, pageable.getPageNumber());
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    public List<Transaction> getTransactionsByUserAndDateRange(Long userId,
                                                               LocalDateTime startDate,
                                                               LocalDateTime endDate) {
        log.debug("Fetching transactions for user {} from {} to {}",
                  userId, startDate, endDate);
        return transactionRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    public List<Transaction> getRecentTransactions(Long userId, int limit) {
        log.debug("Fetching {} recent transactions for user {}", limit, userId);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return transactionRepository.findRecentByUserId(userId, pageable);
    }

    public BigDecimal calculateExpensesByCategory(Long categoryId,
                                                  LocalDateTime startDate,
                                                  LocalDateTime endDate) {
        log.debug("Calculating expenses for category {} from {} to {}",
                  categoryId, startDate, endDate);
        return transactionRepository.sumExpensesByCategoryAndDateRange(categoryId, startDate, endDate);
    }
}
