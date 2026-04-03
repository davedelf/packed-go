package com.packed_go.order_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el Transactional Outbox Pattern.
 * order-service publica eventos hacia event-service.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "packedgo.exchange";
    public static final String OUTBOX_QUEUE = "packedgo.outbox";
    public static final String OUTBOX_ROUTING_KEY = "outbox.event";
    public static final String DLQ_QUEUE = "packedgo.outbox.dlq";
    public static final String DLQ_ROUTING_KEY = "outbox.dlq";

    @Bean
    public DirectExchange packedgoExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue outboxQueue() {
        return new Queue(OUTBOX_QUEUE, true);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ_QUEUE, true);
    }

    @Bean
    public Binding outboxBinding(Queue outboxQueue, DirectExchange packedgoExchange) {
        return BindingBuilder.bind(outboxQueue).to(packedgoExchange).with(OUTBOX_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange packedgoExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(packedgoExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
