package com.daniele.expensetracker.dto;

import com.daniele.expensetracker.domain.Expense;
import com.daniele.expensetracker.domain.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        LocalDate expenseDate,
        String storeName,
        BigDecimal amount,
        ExpenseCategory category,
        String categoryDisplayName,
        String subcategory,
        String description,
        boolean recurring,
        boolean shared,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExpenseResponse fromEntity(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getExpenseDate(),
                expense.getStoreName(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCategory() != null ? expense.getCategory().getDisplayName() : null,
                expense.getSubcategory(),
                expense.getDescription(),
                expense.isRecurring(),
                expense.isShared(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
