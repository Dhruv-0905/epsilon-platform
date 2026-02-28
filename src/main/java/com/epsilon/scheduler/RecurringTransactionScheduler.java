package com.epsilon.scheduler;

import com.epsilon.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduler for automatically processing recurring transactions.
 * 
 * This component runs daily at 2 AM IST and processes all due recurring
 * transaction rules by creating actual Transaction records in Module 1.
 * 
 * Configuration:
 * - Cron expression: 0 0 2 * * * (2 AM daily)
 * - Timezone: Asia/Kolkata (IST)
 * - Can be disabled via: epsilon.scheduler.recurring.enabled=false
 * 
 * For Render Free Tier:
 * - Instance may spin down after inactivity
 * - Use external cron service (cron-job.org) to ping manual trigger endpoint
 * - Endpoint: POST /api/admin/recurring/process-now
 * 
 * @author Epsilon Platform
 * @version 2.0 (Module 2A - The Vault)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "epsilon.scheduler.recurring.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RecurringTransactionScheduler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final RecurringTransactionService recurringTransactionService;
    
    // Track last execution details
    private LocalDateTime lastExecutionTime;
    private int lastProcessedCount;
    private String lastExecutionStatus;

    /**
     * Scheduled job that runs daily at 2 AM IST.
     * 
     * This method:
     * 1. Logs execution start time
     * 2. Calls the service to process due recurring transactions
     * 3. Tracks execution statistics
     * 4. Handles any errors gracefully
     * 
     * Cron format: second minute hour day month weekday
     * 0 0 2 * * * = At 02:00:00 AM every day
     */
    @Scheduled(cron = "${epsilon.scheduler.recurring.cron:0 0 2 * * *}", 
               zone = "${epsilon.scheduler.recurring.timezone:Asia/Kolkata}")
    public void processRecurringTransactions() {
        LocalDateTime executionTime = LocalDateTime.now();
        
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  SCHEDULED JOB: Recurring Transaction Processing");
        log.info("  Execution Time: {}", executionTime.format(FORMATTER));
        log.info("═══════════════════════════════════════════════════════════");

        try {
            int processedCount = recurringTransactionService.processDueRecurringTransactions();
            
            // Update execution tracking
            this.lastExecutionTime = executionTime;
            this.lastProcessedCount = processedCount;
            this.lastExecutionStatus = "SUCCESS";
            
            log.info("═══════════════════════════════════════════════════════════");
            log.info("  SCHEDULED JOB: Completed Successfully");
            log.info("  Processed: {} recurring transaction(s)", processedCount);
            log.info("═══════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            // Update execution tracking with error
            this.lastExecutionTime = executionTime;
            this.lastProcessedCount = 0;
            this.lastExecutionStatus = "FAILED: " + e.getMessage();
            
            log.error("═══════════════════════════════════════════════════════════");
            log.error("  SCHEDULED JOB: Failed");
            log.error("  Error: {}", e.getMessage(), e);
            log.error("═══════════════════════════════════════════════════════════");
        }
    }

    /**
     * Get the last execution time.
     * Used by admin status endpoint.
     * 
     * @return Last execution timestamp or null if never executed
     */
    public LocalDateTime getLastExecutionTime() {
        return lastExecutionTime;
    }

    /**
     * Get the count of recurring transactions processed in last execution.
     * 
     * @return Number of processed transactions
     */
    public int getLastProcessedCount() {
        return lastProcessedCount;
    }

    /**
     * Get the status of last execution (SUCCESS or FAILED with error message).
     * 
     * @return Execution status string
     */
    public String getLastExecutionStatus() {
        return lastExecutionStatus != null ? lastExecutionStatus : "NOT_EXECUTED_YET";
    }
}
