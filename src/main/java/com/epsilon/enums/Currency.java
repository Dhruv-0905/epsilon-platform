package com.epsilon.enums;
/**
 * Supported currencies for the Epsilon platform.
 * Each currency includes its ISO code and symbol for display purposes.
 * 
 * Engineering Note: We store the code (e.g., "USD") in the database, not the ordinal.
 * This makes the DB human-readable and prevents bugs if we reorder the enum later.
 */
public enum Currency {
    USD("USD", "$", "US Dollar"),
    INR("INR", "₹", "Indian Rupee"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    JPY("JPY", "¥", "Japanese Yen"),
    CAD("CAD", "C$", "Candaian Dollar"),
    AUD("AUD", "A$", "Austrilian Dollar"),
    CHF("CHF", "Fr", "Swiss Franc"); 
    
    public final String code;
    public final String symbol;
    public final String displayName;

    Currency(String code, String symbol, String displayName){
        this.code = code;
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public String getCode(){
        return code;
    }

    public String getSymbol(){
        return symbol;
    }

    public String getDisplayName(){
        return displayName;
    }
}

