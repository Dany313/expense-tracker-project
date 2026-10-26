package com.daniele.receiptocr.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseCreateEventMessage(
        UUID eventId,
        String sourceService,
        LocalDate expenseDate,
        String storeName,
        BigDecimal amount,
        String category,
        String subcategory,
        String description,
        Boolean recurring,
        Boolean shared,
        Instant createdAt
) {
    public ExpenseCreateEventMessage(
            UUID eventId,
            String sourceService,
            LocalDate expenseDate,
            String storeName,
            BigDecimal amount,
            String category,
            String subcategory,
            String description,
            Boolean recurring,
            Instant createdAt
    ) {
        this(eventId, sourceService, expenseDate, storeName, amount, category, subcategory, description, recurring, false, createdAt);
    }
}
