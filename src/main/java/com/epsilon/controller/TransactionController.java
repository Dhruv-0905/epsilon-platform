package com.epsilon.controller;

import com.epsilon.dto.ApiResponse;
import com.epsilon.dto.transaction.TransactionCreateRequest;
import com.epsilon.dto.transaction.TransactionResponse;
import com.epsilon.entity.Account;
import com.epsilon.entity.Category;
import com.epsilon.entity.Transaction;
import com.epsilon.service.AccountService;
import com.epsilon.service.CategoryService;
import com.epsilon.service.TransactionService;
import com.epsilon.util.EntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for transaction operations.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create transaction", description = "Create a new transaction (INCOME, EXPENSE, or TRANSFER)")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionCreateRequest request) {
        
        log.info("Creating {} transaction of {} {}", 
                 request.getTransactionType(), request.getAmount(), request.getCurrency());
        
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        
        // Set accounts based on transaction type
        if (request.getFromAccountId() != null) {
            Account fromAccount = accountService.getAccountById(request.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
            transaction.setFromAccount(fromAccount);
        }
        
        if (request.getToAccountId() != null) {
            Account toAccount = accountService.getAccountById(request.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));
            transaction.setToAccount(toAccount);
        }
        
        // Set category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            transaction.setCategory(category);
        }
        
        Transaction savedTransaction = transactionService.createTransaction(transaction);
        TransactionResponse response = EntityMapper.toTransactionResponse(savedTransaction);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Transaction created successfully", response));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID", description = "Retrieve transaction details")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable Long transactionId) {
        log.debug("Fetching transaction: {}", transactionId);
        
        Transaction transaction = transactionService.getTransactionById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
        
        TransactionResponse response = EntityMapper.toTransactionResponse(transaction);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get account transactions", description = "Retrieve transactions for an account with pagination")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAccountTransactions(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Fetching transactions for account: {} (page: {}, size: {})", accountId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<TransactionResponse> transactions = transactionService.getTransactionsByAccountId(accountId, pageable)
            .map(EntityMapper::toTransactionResponse);
        
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/user/{userId}/recent")
    @Operation(summary = "Get recent transactions", description = "Retrieve recent transactions for a user")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getRecentTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.debug("Fetching {} recent transactions for user: {}", limit, userId);
        
        List<TransactionResponse> transactions = transactionService.getRecentTransactions(userId, limit)
            .stream()
            .map(EntityMapper::toTransactionResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/user/{userId}/date-range")
    @Operation(summary = "Get transactions by date range", description = "Retrieve transactions within a date range")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.debug("Fetching transactions for user {} from {} to {}", userId, startDate, endDate);
        
        List<TransactionResponse> transactions = transactionService
            .getTransactionsByUserAndDateRange(userId, startDate, endDate)
            .stream()
            .map(EntityMapper::toTransactionResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
