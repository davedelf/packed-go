package com.packed_go.event_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuración de RabbitMQ para event-service.
 * Escucha eventos de order-service para generar tickets asincrónicamente.
 * 
 * Incluye:
 * - DLQ para mensajes que fallan después de 3 reintentos
 * - Retry con exponential backoff
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "packedgo.exchange";
    public static final String ORDER_PAID_QUEUE = "packedgo.order.paid";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";
    public static final String ORDER_CANCELLED_QUEUE = "packedgo.order.cancelled";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    
    // DLQ configuration
    public static final String TICKET_DLQ_QUEUE = "packedgo.ticket.dlq";
    public static final String TICKET_DLQ_ROUTING_KEY = "ticket.dlq";

    @Bean
    public DirectExchange packedgoExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue orderPaidQueue() {
        // DLQ configurada para mensajes rechazados después de reintentos
        return QueueBuilder.durable(ORDER_PAID_QUEUE)
            .withArgument("x-dead-letter-exchange", EXCHANGE_NAME)
            .withArgument("x-dead-letter-routing-key", TICKET_DLQ_ROUTING_KEY)
            .build();
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue ticketDeadLetterQueue() {
        // DLQ para mensajes de tickets que fallan
        return new Queue(TICKET_DLQ_QUEUE, true);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, DirectExchange packedgoExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(packedgoExchange).with(ORDER_PAID_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, DirectExchange packedgoExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(packedgoExchange).with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding ticketDlqBinding(Queue ticketDeadLetterQueue, DirectExchange packedgoExchange) {
        return BindingBuilder.bind(ticketDeadLetterQueue).to(packedgoExchange).with(TICKET_DLQ_ROUTING_KEY);
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
    
    /**
     * Container factory con retry config para el listener de tickets.
     * Retry: 3 intentos, exponential backoff (2s -> 4s -> 8s)
     */
    @Bean(name = "ticketListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory ticketListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        
        // Configurar retry template
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Retry policy: 3 intentos
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Backoff policy: exponential (2s, 4s, 8s)
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        factory.setRetryTemplate(retryTemplate);
        
        // Después de 3 reintentos, rejection final -> va a DLQ si está configurado
        factory.setDefaultRequeueRejected(false);
        
        return factory;
    }
}
