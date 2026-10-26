package com.daniele.expenseagent.messaging;

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
public class ExpenseAgentRabbitMqConfig {

    public static final String EXPENSE_EXCHANGE = "expense.tracker.exchange";
    public static final String EXPENSE_DLX = "expense.tracker.dlx";

    public static final String AGENT_COMMAND_QUEUE = "agent.command.job.queue";
    public static final String AGENT_COMMAND_ROUTING_KEY = "agent.command.job";
    public static final String AGENT_COMMAND_DLQ = "agent.command.job.dlq";
    public static final String AGENT_COMMAND_DLQ_ROUTING_KEY = "agent.command.job.dlq";

    @Bean
    public TopicExchange expenseTrackerExchange() {
        return new TopicExchange(EXPENSE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange expenseTrackerDeadLetterExchange() {
        return new TopicExchange(EXPENSE_DLX, true, false);
    }

    @Bean
    public Queue agentCommandJobQueue() {
        return QueueBuilder.durable(AGENT_COMMAND_QUEUE)
                .withArgument("x-dead-letter-exchange", EXPENSE_DLX)
                .withArgument("x-dead-letter-routing-key", AGENT_COMMAND_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue agentCommandJobDeadLetterQueue() {
        return QueueBuilder.durable(AGENT_COMMAND_DLQ).build();
    }

    @Bean
    public Binding agentCommandJobBinding(Queue agentCommandJobQueue, TopicExchange expenseTrackerExchange) {
        return BindingBuilder.bind(agentCommandJobQueue)
                .to(expenseTrackerExchange)
                .with(AGENT_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Binding agentCommandJobDlqBinding(Queue agentCommandJobDeadLetterQueue, TopicExchange expenseTrackerDeadLetterExchange) {
        return BindingBuilder.bind(agentCommandJobDeadLetterQueue)
                .to(expenseTrackerDeadLetterExchange)
                .with(AGENT_COMMAND_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
