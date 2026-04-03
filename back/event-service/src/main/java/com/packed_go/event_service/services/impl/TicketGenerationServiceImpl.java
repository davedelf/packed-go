package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dto.OrderPaidEventDTO;
import com.packed_go.event_service.dto.OrderPaidEventDTO.OrderItemDTO;
import com.packed_go.event_service.dtos.ticket.ConsumptionItemDTO;
import com.packed_go.event_service.dtos.ticket.CreateTicketWithConsumptionsRequest;
import com.packed_go.event_service.dtos.ticket.TicketWithConsumptionsResponse;
import com.packed_go.event_service.services.TicketGenerationService;
import com.packed_go.event_service.services.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de generación de tickets.
 * Procesa eventos de ORDER_PAID desde RabbitMQ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketGenerationServiceImpl implements TicketGenerationService {

    private final TicketService ticketService;

    /**
     * Genera tickets para una orden pagada.
     * Itera sobre cada item de la orden y crea los tickets correspondientes.
     */
    @Override
    public void generateTicketsForOrder(OrderPaidEventDTO event) {
        log.info("🎟️ Generando tickets para orden: {}", event.getOrderNumber());
        
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.error("❌ No items found in order event: {}", event.getOrderNumber());
            return;
        }

        int ticketsGenerated = 0;
        int ticketsFailed = 0;

        // Por cada OrderItem (que representa entradas de un evento)
        for (OrderItemDTO item : event.getItems()) {
            Long eventId = item.getEventId();
            Integer quantity = item.getQuantity();
            
            log.info("Generando {} ticket(s) para evento: {}", quantity, eventId);
            
            // Generar un ticket por cada entrada
            for (int i = 0; i < quantity; i++) {
                try {
                    // Preparar las consumiciones si existen
                    List<ConsumptionItemDTO> consumptions = new ArrayList<>();
                    if (item.getConsumptions() != null && !item.getConsumptions().isEmpty()) {
                        consumptions = item.getConsumptions().stream()
                                .map(cons -> ConsumptionItemDTO.builder()
                                        .consumptionId(cons.getConsumptionId())
                                        .consumptionName(cons.getConsumptionName())
                                        .priceAtPurchase(cons.getPriceAtPurchase())
                                        .quantity(cons.getQuantity())
                                        .build())
                                .collect(java.util.stream.Collectors.toList());
                    }
                    
                    // Crear ticket con consumiciones
                    CreateTicketWithConsumptionsRequest ticketRequest = CreateTicketWithConsumptionsRequest.builder()
                            .userId(event.getUserId())
                            .eventId(eventId)
                            .consumptions(consumptions)
                            .build();
                    
                    TicketWithConsumptionsResponse response = ticketService.createTicketWithConsumptions(ticketRequest);
                    
                    if (response != null && response.getTicketId() != null) {
                        ticketsGenerated++;
                        log.info("✅ Ticket #{} generado: ID={}", (i + 1), response.getTicketId());
                    } else {
                        ticketsFailed++;
                        log.error("❌ Falló generar ticket #{}: respuesta nula o sin ID", (i + 1));
                    }
                    
                } catch (Exception e) {
                    ticketsFailed++;
                    log.error("❌ Error generando ticket #{} para evento {}: {}", 
                            (i + 1), eventId, e.getMessage(), e);
                }
            }
        }
        
        log.info("🎟️ Generación de tickets completada para orden {}: {} exitosos, {} fallidos",
                event.getOrderNumber(), ticketsGenerated, ticketsFailed);
        
        if (ticketsFailed > 0) {
            // En un caso real, aquí se podría publicar un evento de compensación
            // o enviar una alerta para intervención manual
            log.warn("⚠️ Algunos tickets falleron en generarse. Intervención manual requerida.");
        }
    }
}
