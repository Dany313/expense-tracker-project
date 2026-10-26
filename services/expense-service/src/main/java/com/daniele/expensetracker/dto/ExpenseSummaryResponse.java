package com.daniele.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ExpenseSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalCount,
        BigDecimal totalAmount,
        BigDecimal averageAmount,
        BigDecimal averageMonthlyAmount,
        BigDecimal currentMonthTotalAmount,
        Map<String, BigDecimal> amountByCategory,
        Map<String, BigDecimal> amountByStore
) {
}
