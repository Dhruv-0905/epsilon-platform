package com.epsilon.enums;

public enum TransactionType {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer"),
    INVESTMENT("Investment"),
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdraw");

    public final String displayName;

    TransactionType(String displayName){
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
