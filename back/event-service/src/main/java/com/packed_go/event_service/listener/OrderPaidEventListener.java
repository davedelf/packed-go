package com.packed_go.event_service.listener;

import com.packed_go.event_service.config.RabbitMQConfig;
import com.packed_go.event_service.dto.OrderPaidEventDTO;
import com.packed_go.event_service.services.TicketGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener de RabbitMQ para procesar eventos de order-service.
 * Genera tickets de forma asíncrona cuando una orden es pagada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidEventListener {

    private final TicketGenerationService ticketGenerationService;

    /**
     * Escucha eventos de ORDER_PAID y genera los tickets correspondientes.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void handleOrderPaidEvent(OrderPaidEventDTO event) {
        log.info("📥 Received ORDER_PAID event for order: {}", event.getOrderNumber());
        
        try {
            ticketGenerationService.generateTicketsForOrder(event);
            log.info("✅ Tickets generated successfully for order: {}", event.getOrderNumber());
        } catch (Exception e) {
            log.error("❌ Failed to generate tickets for order {}: {}", 
                    event.getOrderNumber(), e.getMessage(), e);
            // En un caso real, aquí se podría publicar a una DLQ o crear un evento de compensación
            throw e; // Re-lanzar para que RabbitMQ haga retry
        }
    }
}
