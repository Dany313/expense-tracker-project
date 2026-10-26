package com.daniele.expensetracker.messaging;

import com.daniele.expensetracker.domain.ExpenseCategory;
import com.daniele.expensetracker.dto.ExpenseCreateRequest;
import com.daniele.expensetracker.dto.ExpenseResponse;
import com.daniele.expensetracker.service.ExpenseService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ExpenseQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExpenseQueueConsumer.class);

    private final ExpenseService expenseService;

    public ExpenseQueueConsumer(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @RabbitListener(queues = ExpenseRabbitMqConfig.EXPENSE_CREATE_QUEUE)
    public void onExpenseCreateCommand(ExpenseCreateEventMessage message) {
        if (message == null) {
            throw new AmqpRejectAndDontRequeueException("Messaggio ExpenseCreateEventMessage nullo");
        }
        if (message.storeName() == null || message.storeName().isBlank()) {
            throw new AmqpRejectAndDontRequeueException(
                    "Messaggio ExpenseCreateEventMessage non valido: storeName mancante (eventId=" + message.eventId() + ")"
            );
        }
        if (message.amount() == null || message.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AmqpRejectAndDontRequeueException(
                    "Messaggio ExpenseCreateEventMessage non valido: amount deve essere > 0 (eventId=" + message.eventId() + ")"
            );
        }

        LocalDate date = message.expenseDate() != null ? message.expenseDate() : LocalDate.now();
        ExpenseCategory category = ExpenseCategory.fromFlexibleString(message.category());

        ExpenseCreateRequest createRequest = new ExpenseCreateRequest(
                date,
                message.storeName().trim(),
                message.amount(),
                category,
                message.subcategory(),
                message.description(),
                Boolean.TRUE.equals(message.recurring()),
                Boolean.TRUE.equals(message.shared())
        );

        ExpenseResponse created = expenseService.createExpense(createRequest);
        log.info(
                "Spesa creata da coda RabbitMQ [eventId={}, source={}, id={}, store={}, amount={} EUR]",
                message.eventId(),
                message.sourceService(),
                created.id(),
                created.storeName(),
                created.amount()
        );
    }
}
