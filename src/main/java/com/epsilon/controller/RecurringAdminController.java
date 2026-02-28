package com.epsilon.controller;

import com.epsilon.scheduler.RecurringTransactionScheduler;
import com.epsilon.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for managing recurring transaction processing and monitoring.
 * 
 * Endpoints:
 * - POST /api/admin/recurring/process-now : Manually trigger processing
 * - GET /api/admin/recurring/scheduler/status : Get scheduler health status
 * 
 * Note: These endpoints are PUBLIC for Phase 2A.
 * Security will be added in Module 2 (Security) with @PreAuthorize("hasRole('ADMIN')")
 * 
 * For Render Free Tier:
 * - Use external cron service (e.g., cron-job.org) to ping /process-now daily
 * - This wakes up the instance and ensures processing happens
 * 
 * @author Epsilon Platform
 * @version 2.0 (Module 2A - The Vault)
 */
@RestController
@RequestMapping("/api/admin/recurring")
@RequiredArgsConstructor
@Slf4j
public class RecurringAdminController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final RecurringTransactionService recurringTransactionService;
    private final RecurringTransactionScheduler scheduler;

    /**
     * Manually trigger recurring transaction processing.
     * 
     * Use cases:
     * - Testing during development
     * - Recovery from missed scheduled execution
     * - External cron service trigger (cron-job.org)
     * - Admin needs to force processing outside schedule
     * 
     * This endpoint can be pinged by external services to ensure processing
     * happens even when Render free tier instance is spun down.
     * 
     * @return Response with processing results
     */
    @PostMapping("/process-now")
    public ResponseEntity<Map<String, Object>> triggerManualProcessing() {
        LocalDateTime triggerTime = LocalDateTime.now();
        
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  MANUAL TRIGGER: Recurring Transaction Processing");
        log.info("  Trigger Time: {}", triggerTime.format(FORMATTER));
        log.info("═══════════════════════════════════════════════════════════");

        try {
            int processedCount = recurringTransactionService.processDueRecurringTransactions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Manual processing completed successfully");
            response.put("triggerTime", triggerTime.format(FORMATTER));
            response.put("processedCount", processedCount);
            response.put("timezone", "Asia/Kolkata (IST)");
            
            log.info("═══════════════════════════════════════════════════════════");
            log.info("  MANUAL TRIGGER: Completed Successfully");
            log.info("  Processed: {} recurring transaction(s)", processedCount);
            log.info("═══════════════════════════════════════════════════════════");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAILED");
            response.put("message", "Manual processing failed");
            response.put("error", e.getMessage());
            response.put("triggerTime", triggerTime.format(FORMATTER));
            
            log.error("═══════════════════════════════════════════════════════════");
            log.error("  MANUAL TRIGGER: Failed");
            log.error("  Error: {}", e.getMessage(), e);
            log.error("═══════════════════════════════════════════════════════════");
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get scheduler status and last execution information.
     * 
     * Returns:
     * - Scheduler enabled status
     * - Last execution time
     * - Last execution status (SUCCESS/FAILED)
     * - Number of transactions processed in last run
     * - Next scheduled execution time (if available)
     * 
     * @return Scheduler health status
     */
    @GetMapping("/scheduler/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        LocalDateTime lastExecution = scheduler.getLastExecutionTime();
        
        status.put("schedulerEnabled", true); // If this endpoint responds, scheduler is enabled
        status.put("currentTime", LocalDateTime.now().format(FORMATTER));
        status.put("timezone", "Asia/Kolkata (IST)");
        status.put("scheduleCron", "0 0 2 * * * (2 AM IST daily)");
        
        if (lastExecution != null) {
            status.put("lastExecutionTime", lastExecution.format(FORMATTER));
            status.put("lastExecutionStatus", scheduler.getLastExecutionStatus());
            status.put("lastProcessedCount", scheduler.getLastProcessedCount());
        } else {
            status.put("lastExecutionTime", "NOT_EXECUTED_YET");
            status.put("lastExecutionStatus", "WAITING_FOR_FIRST_RUN");
            status.put("lastProcessedCount", 0);
        }
        
        // Calculate next execution (approximately)
        // For 2 AM daily schedule
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecution = now.withHour(2).withMinute(0).withSecond(0);
        if (now.getHour() >= 2) {
            nextExecution = nextExecution.plusDays(1);
        }
        status.put("nextScheduledExecution", nextExecution.format(FORMATTER) + " (approximate)");
        
        log.debug("Scheduler status requested: {}", status);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Health check endpoint specific to recurring transaction scheduler.
     * 
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getHealth() {
        Map<String, String> health = new HashMap<>();
        health.put("service", "RecurringTransactionScheduler");
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().format(FORMATTER));
        return ResponseEntity.ok(health);
    }
}
