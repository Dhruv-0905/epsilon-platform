package com.epsilon.service;

import com.epsilon.entity.Account;
import com.epsilon.entity.User;
import com.epsilon.enums.AccountType;
import com.epsilon.enums.Currency;
import com.epsilon.repository.AccountRepository;
import com.epsilon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer for Account operations.
 * Manages bank accounts and balances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    /**
     * Create a new account for a user.
     *
     * Business Rules:
     * 1. User must exist
     * 2. Account number must be unique (auto-generated if missing)
     * 3. Initial balance defaults to 0
     * 4. Currency defaults to user's default currency
     */
    @Transactional
    public Account createAccount(Long userId, Account account) {
        log.info("Creating account for user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        account.setUser(user);

        if (account.getAccountNumber() == null || account.getAccountNumber().isEmpty()) {
            account.setAccountNumber(generateAccountNumber());
        } else {
            if (accountRepository.existsByAccountNumber(account.getAccountNumber())) {
                throw new IllegalArgumentException("Account number already exists: " + account.getAccountNumber());
            }
        }

        if (account.getCurrency() == null) {
            account.setCurrency(user.getDefaultCurrency());
            log.debug("Currency not provided, using user's default: {}", user.getDefaultCurrency());
        }

        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }

        if (account.getIsActive() == null) {
            account.setIsActive(true);
        }

        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully with ID: {} and number: {}",
                 savedAccount.getId(), savedAccount.getAccountNumber());
        return savedAccount;
    }

    /**
     * Generate a unique 8-digit account number.
     * Uses collision detection and a max-attempts guard.
     */
    private String generateAccountNumber() {
        String accountNumber;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            accountNumber = String.format("%08d", (int) (Math.random() * 100000000));
            attempts++;

            if (attempts >= maxAttempts) {
                log.error("Failed to generate unique account number after {} attempts", maxAttempts);
                throw new IllegalStateException("Unable to generate unique account number. Please try again.");
            }
        } while (accountRepository.existsByAccountNumber(accountNumber));

        log.debug("Generated unique account number: {} (attempts: {})", accountNumber, attempts);
        return accountNumber;
    }

    public Optional<Account> getAccountById(Long accountId) {
        log.debug("Fetching account with ID: {}", accountId);
        return accountRepository.findById(accountId);
    }

    public List<Account> getAccountsByUserId(Long userId) {
        log.debug("Fetching accounts for user ID: {}", userId);
        return accountRepository.findByUserId(userId);
    }

    public List<Account> getActiveAccountsByUserId(Long userId) {
        log.debug("Fetching active accounts for user ID: {}", userId);
        return accountRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public List<Account> getAccountsByType(Long userId, AccountType accountType) {
        log.debug("Fetching {} accounts for user ID: {}", accountType, userId);
        return accountRepository.findByUserIdAndAccountType(userId, accountType);
    }

    /**
     * Get total balances grouped by currency.
     */
    public Map<Currency, BigDecimal> getTotalBalancesByCurrency(Long userId) {
        log.debug("Calculating total balances by currency for user ID: {}", userId);

        List<Account> accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        Map<Currency, BigDecimal> balancesByCurrency = new HashMap<>();

        for (Account account : accounts) {
            Currency currency = account.getCurrency();
            BigDecimal currentTotal = balancesByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            BigDecimal newTotal = currentTotal.add(account.getBalance());
            balancesByCurrency.put(currency, newTotal);
        }

        log.debug("Total balances for user {}: {}", userId, balancesByCurrency);
        return balancesByCurrency;
    }

    /**
     * Low-level balance update. Normal flows should use TransactionService.
     */
    @Transactional
    public Account updateBalance(Long accountId, BigDecimal newBalance) {
        log.info("Updating balance for account ID: {} to {}", accountId, newBalance);

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        if (newBalance.compareTo(BigDecimal.ZERO) < 0 &&
            account.getAccountType() != AccountType.CREDIT_CARD) {
            throw new IllegalArgumentException("Balance cannot be negative for " + account.getAccountType());
        }

        BigDecimal oldBalance = account.getBalance();
        account.setBalance(newBalance);

        Account saved = accountRepository.save(account);
        log.info("Balance updated from {} to {} for account {}", oldBalance, newBalance, accountId);
        return saved;
    }

    /**
     * Soft-deactivate an account.
     * Requires zero balance to avoid "orphaned" money.
     */
    @Transactional
    public void deactivateAccount(Long accountId) {
        log.info("Deactivating account with ID: {}", accountId);

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                String.format("Cannot deactivate account with non-zero balance. Current balance: %s %s",
                              account.getBalance(), account.getCurrency())
            );
        }

        account.setIsActive(false);
        accountRepository.save(account);
        log.info("Account deactivated successfully: {}", accountId);
    }

    /**
     * Ownership guard for security checks.
     */
    public boolean verifyAccountOwnership(Long accountId, Long userId) {
        return accountRepository.findById(accountId)
            .map(account -> account.getUser().getId().equals(userId))
            .orElse(false);
    }
}
