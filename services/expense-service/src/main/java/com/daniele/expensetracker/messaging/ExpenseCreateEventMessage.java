package com.daniele.expensetracker.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Messaggio AMQP pubblicato sulla coda {@code expense.create.queue} dai microservizi AI
 * (es. {@code receipt-ocr-service} o {@code expense-agent-service}) per salvare una spesa
 * su PostgreSQL in modo asincrono e resiliente.
 */
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
        Boolean shared
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
            Boolean recurring
    ) {
        this(eventId, sourceService, expenseDate, storeName, amount, category, subcategory, description, recurring, false);
    }
}
