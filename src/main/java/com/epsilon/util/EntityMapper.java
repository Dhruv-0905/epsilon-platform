package com.epsilon.util;

import com.epsilon.dto.account.AccountResponse;
import com.epsilon.dto.category.CategoryResponse;
import com.epsilon.dto.recurring.RecurringTransactionResponse;
import com.epsilon.dto.transaction.TransactionResponse;
import com.epsilon.dto.user.UserResponse;
import com.epsilon.entity.*;

/**
 * Utility class for mapping entities to DTOs.
 * Prevents Jackson serialization issues with lazy-loaded relationships.
 */
public class EntityMapper {

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getDefaultCurrency(),
            user.getIsActive(),
            user.getCreatedAt()
        );
    }

    public static AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getAccountName(),
            account.getAccountNumber(),
            account.getAccountType(),
            account.getCurrency(),
            account.getBalance(),
            account.getBankName(),
            account.getIsActive(),
            account.getCreatedAt()
        );
    }

    public static TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getTransactionType(),
            transaction.getDescription(),
            transaction.getFromAccount() != null ? transaction.getFromAccount().getId() : null,
            transaction.getFromAccount() != null ? transaction.getFromAccount().getAccountName() : null,
            transaction.getToAccount() != null ? transaction.getToAccount().getId() : null,
            transaction.getToAccount() != null ? transaction.getToAccount().getAccountName() : null,
            transaction.getCategory() != null ? transaction.getCategory().getId() : null,
            transaction.getCategory() != null ? transaction.getCategory().getCategoryName() : null,
            transaction.getTransactionDate(),
            transaction.getCreatedAt()
        );
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getCategoryName(),
            category.getDescription(),
            category.getColorCode(),
            category.getIsActive(),
            category.getCreatedAt()
        );
    }

    public static RecurringTransactionResponse toRecurringTransactionResponse(RecurringTransaction recurring) {
        return new RecurringTransactionResponse(
            recurring.getId(),
            recurring.getAccount().getId(),
            recurring.getAccount().getAccountName(),
            recurring.getAmount(),
            recurring.getCurrency(),
            recurring.getTransactionType(),
            recurring.getFrequency(),
            recurring.getDescription(),
            recurring.getStartDate(),
            recurring.getEndDate(),
            recurring.getNextRunDate(),
            recurring.getCategory() != null ? recurring.getCategory().getId() : null,
            recurring.getCategory() != null ? recurring.getCategory().getCategoryName() : null,
            recurring.getIsActive(),
            recurring.getCreatedAt()
        );
    }
}
