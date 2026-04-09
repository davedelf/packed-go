package com.packed_go.event_service.listener;

import com.packed_go.event_service.config.RabbitMQConfig;
import com.packed_go.event_service.dto.OrderPaidEventDTO;
import com.packed_go.event_service.services.TicketGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Listener de RabbitMQ para procesar eventos de order-service.
 * Genera tickets de forma asíncrona cuando una orden es pagada.
 * 
 * Configuración de retry:
 * - 3 reintentos con exponential backoff (2s, 4s, 8s)
 * - Después de 3 fallos, el mensaje va a DLQ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidEventListener {

    private final TicketGenerationService ticketGenerationService;

    /**
     * Escucha eventos de ORDER_PAID y genera los tickets correspondientes.
     * Retry: 3 intentos con exponential backoff
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE, containerFactory = "ticketListenerContainerFactory")
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void handleOrderPaidEvent(OrderPaidEventDTO event) {
        log.info("📥 Received ORDER_PAID event for order: {}", event.getOrderNumber());
        
        try {
            ticketGenerationService.generateTicketsForOrder(event);
            log.info("✅ Tickets generated successfully for order: {}", event.getOrderNumber());
        } catch (Exception e) {
            log.error("❌ Failed to generate tickets for order {}: {}", 
                    event.getOrderNumber(), e.getMessage(), e);
            throw e; // Re-lanzar para que Retry haga su trabajo
        }
    }
}
