package com.epsilon.enums;

/**
 * Represents the outcome of a single recurring transaction execution.
 *
 * SUCCESS - Transaction was created and balance updated successfully.
 * FAILED  - An error occurred; no transaction was created. Error stored in execution record.
 */
public enum ExecutionStatus {
    SUCCESS,
    FAILED,
    SKIPPED
}
