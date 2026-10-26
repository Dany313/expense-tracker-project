package com.daniele.receiptocr.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class ReceiptOcrRabbitMqConfig {

    public static final String EXPENSE_EXCHANGE = "expense.tracker.exchange";
    public static final String EXPENSE_DLX = "expense.tracker.dlx";

    public static final String RECEIPT_OCR_QUEUE = "receipt.ocr.job.queue";
    public static final String RECEIPT_OCR_ROUTING_KEY = "receipt.ocr.job";
    public static final String RECEIPT_OCR_DLQ = "receipt.ocr.job.dlq";
    public static final String RECEIPT_OCR_DLQ_ROUTING_KEY = "receipt.ocr.job.dlq";

    public static final String EXPENSE_CREATE_QUEUE = "expense.create.queue";
    public static final String EXPENSE_CREATE_ROUTING_KEY = "expense.create";
    public static final String EXPENSE_CREATE_DLQ = "expense.create.dlq";
    public static final String EXPENSE_CREATE_DLQ_ROUTING_KEY = "expense.create.dlq";

    @Bean
    public TopicExchange expenseTrackerExchange() {
        return new TopicExchange(EXPENSE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange expenseTrackerDeadLetterExchange() {
        return new TopicExchange(EXPENSE_DLX, true, false);
    }

    @Bean
    public Queue receiptOcrJobQueue() {
        return QueueBuilder.durable(RECEIPT_OCR_QUEUE)
                .withArgument("x-dead-letter-exchange", EXPENSE_DLX)
                .withArgument("x-dead-letter-routing-key", RECEIPT_OCR_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue receiptOcrJobDeadLetterQueue() {
        return QueueBuilder.durable(RECEIPT_OCR_DLQ).build();
    }

    @Bean
    public Binding receiptOcrJobBinding(Queue receiptOcrJobQueue, TopicExchange expenseTrackerExchange) {
        return BindingBuilder.bind(receiptOcrJobQueue)
                .to(expenseTrackerExchange)
                .with(RECEIPT_OCR_ROUTING_KEY);
    }

    @Bean
    public Binding receiptOcrJobDlqBinding(Queue receiptOcrJobDeadLetterQueue, TopicExchange expenseTrackerDeadLetterExchange) {
        return BindingBuilder.bind(receiptOcrJobDeadLetterQueue)
                .to(expenseTrackerDeadLetterExchange)
                .with(RECEIPT_OCR_DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue expenseCreateQueue() {
        return QueueBuilder.durable(EXPENSE_CREATE_QUEUE)
                .withArgument("x-dead-letter-exchange", EXPENSE_DLX)
                .withArgument("x-dead-letter-routing-key", EXPENSE_CREATE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue expenseCreateDeadLetterQueue() {
        return QueueBuilder.durable(EXPENSE_CREATE_DLQ).build();
    }

    @Bean
    public Binding expenseCreateBinding(Queue expenseCreateQueue, TopicExchange expenseTrackerExchange) {
        return BindingBuilder.bind(expenseCreateQueue)
                .to(expenseTrackerExchange)
                .with(EXPENSE_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding expenseCreateDlqBinding(Queue expenseCreateDeadLetterQueue, TopicExchange expenseTrackerDeadLetterExchange) {
        return BindingBuilder.bind(expenseCreateDeadLetterQueue)
                .to(expenseTrackerDeadLetterExchange)
                .with(EXPENSE_CREATE_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
