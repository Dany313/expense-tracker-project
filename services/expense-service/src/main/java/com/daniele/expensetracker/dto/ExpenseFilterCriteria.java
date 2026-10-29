package com.daniele.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseFilterCriteria(
        LocalDate startDate,
        LocalDate endDate,
        String storeName,
        String category,
        String subcategory,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Boolean recurring,
        Boolean shared
) {
    public ExpenseFilterCriteria(
            LocalDate startDate,
            LocalDate endDate,
            String storeName,
            String category,
            String subcategory,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Boolean recurring
    ) {
        this(startDate, endDate, storeName, category, subcategory, minAmount, maxAmount, recurring, null);
    }

    public static ExpenseFilterCriteria empty() {
        return new ExpenseFilterCriteria(null, null, null, null, null, null, null, null, null);
    }
}
