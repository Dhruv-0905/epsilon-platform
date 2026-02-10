package com.epsilon.controller;

import com.epsilon.dto.ApiResponse;
import com.epsilon.dto.recurring.RecurringTransactionRequest;
import com.epsilon.dto.recurring.RecurringTransactionResponse;
import com.epsilon.entity.Account;
import com.epsilon.entity.Category;
import com.epsilon.entity.RecurringTransaction;
import com.epsilon.service.AccountService;
import com.epsilon.service.CategoryService;
import com.epsilon.service.RecurringTransactionService;
import com.epsilon.util.EntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for recurring transaction operations.
 */
@RestController
@RequestMapping("/api/recurring-transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recurring Transactions", description = "Recurring transaction management endpoints")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @PostMapping("/user/{userId}")
    @Operation(summary = "Create recurring transaction", description = "Create a new recurring transaction rule")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> createRecurringTransaction(
            @PathVariable Long userId,
            @Valid @RequestBody RecurringTransactionRequest request) {
        
        log.info("Creating recurring transaction for user: {}", userId);
        
        Account account = accountService.getAccountById(request.getAccountId())
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setAccount(account);
        recurring.setAmount(request.getAmount());
        recurring.setCurrency(request.getCurrency());
        recurring.setTransactionType(request.getTransactionType());
        recurring.setFrequency(request.getFrequency());
        recurring.setDescription(request.getDescription());
        recurring.setStartDate(request.getStartDate());
        recurring.setEndDate(request.getEndDate());
        
        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            recurring.setCategory(category);
        }
        
        RecurringTransaction saved = recurringService.createRecurringTransaction(userId, recurring);
        RecurringTransactionResponse response = EntityMapper.toRecurringTransactionResponse(saved);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Recurring transaction created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recurring transaction by ID", description = "Retrieve recurring transaction details")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> getRecurringTransactionById(@PathVariable Long id) {
        log.debug("Fetching recurring transaction: {}", id);
        
        RecurringTransaction recurring = recurringService.getRecurringTransactionById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));
        
        RecurringTransactionResponse response = EntityMapper.toRecurringTransactionResponse(recurring);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user recurring transactions", description = "Retrieve all recurring transactions for a user")
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponse>>> getUserRecurringTransactions(
            @PathVariable Long userId) {
        
        log.debug("Fetching recurring transactions for user: {}", userId);
        
        List<RecurringTransactionResponse> recurring = recurringService.getRecurringTransactionsByUserId(userId)
            .stream()
            .map(EntityMapper::toRecurringTransactionResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(recurring));
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Get active recurring transactions", description = "Retrieve active recurring transactions for a user")
    public ResponseEntity<ApiResponse<List<RecurringTransactionResponse>>> getActiveRecurringTransactions(
            @PathVariable Long userId) {
        
        log.debug("Fetching active recurring transactions for user: {}", userId);
        
        List<RecurringTransactionResponse> recurring = recurringService.getActiveRecurringTransactionsByUserId(userId)
            .stream()
            .map(EntityMapper::toRecurringTransactionResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(recurring));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update recurring transaction", description = "Update recurring transaction details")
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> updateRecurringTransaction(
            @PathVariable Long id,
            @Valid @RequestBody RecurringTransactionRequest request) {
        
        log.info("Updating recurring transaction: {}", id);
        
        RecurringTransaction updateData = new RecurringTransaction();
        updateData.setAmount(request.getAmount());
        updateData.setFrequency(request.getFrequency());
        updateData.setDescription(request.getDescription());
        updateData.setEndDate(request.getEndDate());
        
        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            updateData.setCategory(category);
        }
        
        RecurringTransaction updated = recurringService.updateRecurringTransaction(id, updateData);
        RecurringTransactionResponse response = EntityMapper.toRecurringTransactionResponse(updated);
        
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate recurring transaction", description = "Pause/deactivate a recurring transaction")
    public ResponseEntity<ApiResponse<Void>> deactivateRecurringTransaction(@PathVariable Long id) {
        log.info("Deactivating recurring transaction: {}", id);
        
        recurringService.deactivateRecurringTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Recurring transaction deactivated successfully", null));
    }
}
