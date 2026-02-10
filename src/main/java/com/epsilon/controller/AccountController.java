package com.epsilon.controller;

import com.epsilon.dto.ApiResponse;
import com.epsilon.dto.account.AccountCreateRequest;
import com.epsilon.dto.account.AccountResponse;
import com.epsilon.dto.account.BalanceSummaryResponse;
import com.epsilon.entity.Account;
import com.epsilon.enums.Currency;
import com.epsilon.service.AccountService;
import com.epsilon.util.EntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for account operations.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounts", description = "Account management endpoints")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/user/{userId}")
    @Operation(summary = "Create account", description = "Create a new account for a user")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @PathVariable Long userId,
            @Valid @RequestBody AccountCreateRequest request) {
        
        log.info("Creating account for user: {}", userId);
        
        Account account = new Account();
        account.setAccountName(request.getAccountName());
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());
        account.setBalance(request.getBalance());
        account.setBankName(request.getBankName());
        
        Account savedAccount = accountService.createAccount(userId, account);
        AccountResponse response = EntityMapper.toAccountResponse(savedAccount);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Account created successfully", response));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID", description = "Retrieve account details")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable Long accountId) {
        log.debug("Fetching account: {}", accountId);
        
        Account account = accountService.getAccountById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));
        
        AccountResponse response = EntityMapper.toAccountResponse(account);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user accounts", description = "Retrieve all accounts for a user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getUserAccounts(@PathVariable Long userId) {
        log.debug("Fetching accounts for user: {}", userId);
        
        List<AccountResponse> accounts = accountService.getAccountsByUserId(userId)
            .stream()
            .map(EntityMapper::toAccountResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Get active accounts", description = "Retrieve all active accounts for a user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getActiveAccounts(@PathVariable Long userId) {
        log.debug("Fetching active accounts for user: {}", userId);
        
        List<AccountResponse> accounts = accountService.getActiveAccountsByUserId(userId)
            .stream()
            .map(EntityMapper::toAccountResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/user/{userId}/balance-summary")
    @Operation(summary = "Get balance summary", description = "Get total balances grouped by currency")
    public ResponseEntity<ApiResponse<BalanceSummaryResponse>> getBalanceSummary(@PathVariable Long userId) {
        log.debug("Fetching balance summary for user: {}", userId);
        
        Map<Currency, BigDecimal> balances = accountService.getTotalBalancesByCurrency(userId);
        int totalAccounts = accountService.getActiveAccountsByUserId(userId).size();
        
        BalanceSummaryResponse summary = new BalanceSummaryResponse(balances, totalAccounts);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Deactivate account", description = "Soft-delete an account (requires zero balance)")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@PathVariable Long accountId) {
        log.info("Deactivating account: {}", accountId);
        
        accountService.deactivateAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully", null));
    }
}
