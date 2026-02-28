package com.epsilon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for Spring Task Scheduling.
 * 
 * This configuration:
 * - Enables scheduling framework with @EnableScheduling
 * - Creates a thread pool for scheduled tasks
 * - Configures thread naming for better log tracking
 * - Sets pool size appropriate for recurring transaction processing
 * 
 * Thread Pool Configuration:
 * - Pool size: 2 (one for scheduled tasks, one for manual triggers)
 * - Thread name prefix: "epsilon-scheduler-"
 * - Shutdown: Waits for tasks to complete before shutdown
 * 
 * @author Epsilon Platform
 * @version 2.0 (Module 2A - The Vault)
 */
@Configuration
@EnableScheduling
@Slf4j
public class SchedulerConfig {

    /**
     * Creates a thread pool task scheduler for executing scheduled jobs.
     * 
     * This scheduler is used by:
     * - @Scheduled methods (RecurringTransactionScheduler)
     * - Manual trigger endpoints (async processing if needed)
     * 
     * Configuration is optimized for Render free tier (low resource usage).
     * 
     * @return Configured TaskScheduler
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // Pool size: 2 threads (low resource usage for free tier)
        scheduler.setPoolSize(2);
        
        // Thread naming for log clarity
        scheduler.setThreadNamePrefix("epsilon-scheduler-");
        
        // Graceful shutdown: wait for tasks to complete
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        
        // Initialize the scheduler
        scheduler.initialize();
        
        log.info("✓ Task Scheduler configured successfully");
        log.info("  - Pool Size: 2");
        log.info("  - Thread Prefix: epsilon-scheduler-");
        log.info("  - Graceful Shutdown: enabled (60s timeout)");
        
        return scheduler;
    }
}
