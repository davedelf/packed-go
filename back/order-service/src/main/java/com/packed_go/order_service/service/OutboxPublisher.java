package com.packed_go.order_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.packed_go.order_service.config.RabbitMQConfig;
import com.packed_go.order_service.entity.OutboxEvent;
import com.packed_go.order_service.entity.OutboxEvent.EventStatus;
import com.packed_go.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduler que publica eventos pendientes de la tabla outbox a RabbitMQ.
 * Se ejecuta cada 5 segundos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publica eventos pendientes a RabbitMQ.
     * Se ejecuta cada 5 segundos.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEventsToProcess();

        if (pendingEvents.isEmpty()) {
            log.debug("No pending events to publish");
            return;
        }

        log.info("Found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
                event.markAsPublished();
                outboxEventRepository.save(event);
                log.info("Evento {} publicado exitosamente", event.getId());
            } catch (Exception e) {
                log.error("Error publicando evento {}: {}", event.getId(), e.getMessage());
                event.markAsFailed(e.getMessage());
                outboxEventRepository.save(event);
            }
        }
    }

    /**
     * Publica un evento específico a RabbitMQ
     */
    private void publishEvent(OutboxEvent event) {
        String routingKey = getRoutingKeyForEventType(event.getEventType());
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                routingKey,
                event.getPayload()
        );

        log.info("Publicado evento {} (type={}) a RabbitMQ con routing key: {}", 
                event.getId(), event.getEventType(), routingKey);
    }

    /**
     * Obtiene la routing key según el tipo de evento
     */
    private String getRoutingKeyForEventType(OutboxEvent.EventType eventType) {
        return switch (eventType) {
            case ORDER_PAID -> "order.paid";
            case ORDER_CANCELLED -> "order.cancelled";
            case TICKETS_CREATED -> "tickets.created";
            case TICKETS_FAILED -> "tickets.failed";
        };
    }
}
