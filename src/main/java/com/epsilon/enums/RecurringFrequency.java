package com.epsilon.enums;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Defines how often a recurring transaction repeats.
 * Each enum knows how to calculate the next occurrence date.
 */
public enum RecurringFrequency {
    DAILY(1, ChronoUnit.DAYS),
    WEEKLY(7, ChronoUnit.DAYS),
    BIWEEKLY(14, ChronoUnit.DAYS),
    MONTHLY(1, ChronoUnit.MONTHS),
    QUARTERLY(3, ChronoUnit.MONTHS),
    YEARLY(1, ChronoUnit.YEARS);

    private final long amount;
    private final ChronoUnit unit;

    RecurringFrequency(long amount, ChronoUnit unit) {
        this.amount = amount;
        this.unit = unit;
    }

    /**
     * Calculate the next occurrence date from a given date.
     * Example: MONTHLY.getNextDate(2024-01-15) → 2024-02-15
     */
    public LocalDate getNextDate(LocalDate currentDate) {
        return currentDate.plus(amount, unit);
    }
}
