package com.epsilon.controller;

import com.epsilon.dto.ApiResponse;
import com.epsilon.dto.recurring.CreateFromTemplateRequest;
import com.epsilon.dto.recurring.ExecutionHistoryResponse;
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
import com.epsilon.dto.recurring.ExecutionHistoryResponse;

import java.util.Map;
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

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2C: Pause / Resume
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Temporarily pause a recurring transaction rule.
     * The scheduler will skip this rule until it is resumed.
     * nextRunDate does not advance while paused.
     *
     * Example: POST /api/recurring-transactions/1/pause?reason=On+vacation
     */
    @PostMapping("/{id}/pause")
    @Operation(
        summary = "Pause recurring transaction",
        description = "Temporarily pause a rule. Scheduler skips it until resumed. nextRunDate does not advance."
    )
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> pauseRecurringTransaction(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        log.info("Pausing recurring transaction: {}", id);
        RecurringTransaction paused = recurringService.pauseRecurringTransaction(id, reason);
        return ResponseEntity.ok(ApiResponse.success(
            "Recurring transaction paused successfully", EntityMapper.toRecurringTransactionResponse(paused)));
    }

    /**
     * Resume a previously paused recurring transaction rule.
     * nextRunDate is reset to today if it fell behind during the pause.
     *
     * Example: POST /api/recurring-transactions/1/resume
     */
    @PostMapping("/{id}/resume")
    @Operation(
        summary = "Resume recurring transaction",
        description = "Resume a paused rule. nextRunDate is reset to today if it fell behind during the pause."
    )
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> resumeRecurringTransaction(
            @PathVariable Long id) {

        log.info("Resuming recurring transaction: {}", id);
        RecurringTransaction resumed = recurringService.resumeRecurringTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(
            "Recurring transaction resumed successfully", EntityMapper.toRecurringTransactionResponse(resumed)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2C: Templates
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get all pre-defined recurring transaction templates.
     * Returns templateId, name, transactionType, frequency and suggestedDescription.
     * Pass templateId to POST /from-template to create a rule instantly.
     *
     * Example: GET /api/recurring-transactions/templates
     */
    @GetMapping("/templates")
    @Operation(
        summary = "Get recurring transaction templates",
        description = "Returns pre-defined templates (salary, rent, groceries etc). Use templateId with POST /from-template."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTemplates() {
        log.debug("Fetching recurring transaction templates");
        return ResponseEntity.ok(ApiResponse.success(recurringService.getTemplates()));
    }

    /**
     * Create a recurring transaction rule from a pre-defined template.
     * Template supplies transactionType and frequency.
     * User supplies amount, currency, accountId and dates.
     *
     * Example: POST /api/recurring-transactions/user/1/from-template
     */
    @PostMapping("/user/{userId}/from-template")
    @Operation(
        summary = "Create from template",
        description = "Create a recurring transaction rule using a pre-defined template. Call GET /templates first for valid templateIds."
    )
    public ResponseEntity<ApiResponse<RecurringTransactionResponse>> createFromTemplate(
            @PathVariable Long userId,
            @Valid @RequestBody CreateFromTemplateRequest request) {

        log.info("Creating recurring transaction from template '{}' for user: {}",
                 request.getTemplateId(), userId);

        RecurringTransaction saved = recurringService.createFromTemplate(
            userId,
            request.getTemplateId(),
            request.getAccountId(),
            request.getAmount(),
            request.getCurrency(),
            request.getStartDate(),
            request.getEndDate(),
            request.getCategoryId()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Recurring transaction created from template", 
                  EntityMapper.toRecurringTransactionResponse(saved)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2B: Execution History & Audit Trail
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get the full execution history for a specific recurring transaction rule.
     *
     * Returns all past executions ordered by date descending.
     * Each entry shows whether the run succeeded or failed, the transaction
     * created (if any), and the amount processed.
     *
     * Example: GET /api/recurring-transactions/1/history
     */
    @GetMapping("/{id}/history")
    @Operation(
        summary = "Get execution history",
        description = "Retrieve full audit trail of all executions for a recurring transaction rule"
    )
    public ResponseEntity<ApiResponse<List<ExecutionHistoryResponse>>> getExecutionHistory(
            @PathVariable Long id) {

        log.debug("Fetching execution history for recurring transaction: {}", id);

        recurringService.getRecurringTransactionById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));

        List<ExecutionHistoryResponse> history = recurringService.getExecutionHistory(id)
            .stream()
            .map(EntityMapper::toExecutionHistoryResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    /**
     * Get execution statistics for a specific recurring transaction rule.
     *
     * Returns:
     * - totalExecutions, successCount, failedCount, successRate
     *
     * Example: GET /api/recurring-transactions/1/stats
     */
    @GetMapping("/{id}/stats")
    @Operation(
        summary = "Get execution stats",
        description = "Retrieve success rate and execution counts for a recurring transaction rule"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionStats(
            @PathVariable Long id) {

        log.debug("Fetching execution stats for recurring transaction: {}", id);

        recurringService.getRecurringTransactionById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recurring transaction not found with ID: " + id));

        Map<String, Object> stats = recurringService.getExecutionStats(id);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

}
