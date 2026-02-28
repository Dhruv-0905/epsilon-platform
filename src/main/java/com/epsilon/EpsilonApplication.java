package com.epsilon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Epsilon Platform.
 * 
 * Epsilon is a GenAI-powered financial management platform.
 * 
 * Modules:
 * - Module 0: Foundation & Infrastructure (COMPLETE)
 * - Module 1: The Ledger - Core financial tracking (COMPLETE)
 * - Module 2: The Vault - Recurring transactions & automation (IN PROGRESS - Phase 2A)
 * 
 * Technology Stack:
 * - Java 17
 * - Spring Boot 3.2
 * - PostgreSQL (NeonDB)
 * - Docker + Render Deployment
 * 
 * @author Epsilon Platform
 * @version 2.0 (Module 2A)
 */
@SpringBootApplication
public class EpsilonApplication {
    public static void main(String[] args) {
        SpringApplication.run(EpsilonApplication.class, args);
    }
}
