package com.epsilon.dto.account;

import com.epsilon.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for total balance summary grouped by currency.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSummaryResponse {
    private Map<Currency, BigDecimal> balancesByCurrency;
    private int totalAccounts;
}
