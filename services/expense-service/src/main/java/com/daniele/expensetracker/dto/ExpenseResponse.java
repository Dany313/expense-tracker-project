package com.daniele.expensetracker.dto;

import com.daniele.expensetracker.domain.Expense;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        LocalDate expenseDate,
        String storeName,
        BigDecimal amount,
        String category,
        String categoryDisplayName,
        String subcategory,
        String description,
        boolean recurring,
        boolean shared,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExpenseResponse fromEntity(Expense expense, String categoryDisplayName) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getExpenseDate(),
                expense.getStoreName(),
                expense.getAmount(),
                expense.getCategory(),
                categoryDisplayName != null ? categoryDisplayName : expense.getCategory(),
                expense.getSubcategory(),
                expense.getDescription(),
                expense.isRecurring(),
                expense.isShared(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
