package com.packed_go.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.packed_go.order_service.entity.OutboxEvent;
import com.packed_go.order_service.entity.OutboxEvent.EventStatus;
import com.packed_go.order_service.entity.OutboxEvent.EventType;
import com.packed_go.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para escribir eventos en la tabla outbox.
 * Se usa en la misma transacción que la orden para garantizar atomicidad.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crea un evento en la tabla outbox dentro de la misma transacción.
     * IMPORTANTE: Debe llamarse desde el mismo método transaccional que modifica la orden.
     *
     * @param aggregateId El ID de la orden (orderNumber)
     * @param eventType   El tipo de evento
     * @param payload     Los datos del evento en forma de objeto
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createEvent(String aggregateId, EventType eventType, Object payload) {
        try {
            // Verificar idempotencia: no crear evento duplicado
            if (outboxEventRepository.existsByAggregateIdAndEventType(aggregateId, eventType)) {
                log.warn("Evento {} ya existe para aggregateId {}, omitiendo", eventType, aggregateId);
                return;
            }

            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(EventStatus.PENDING)
                    .build();

            outboxEventRepository.save(event);
            log.info("Evento {} creado en outbox para aggregateId: {}", eventType, aggregateId);

        } catch (JsonProcessingException e) {
            log.error("Error serializando payload para outbox event: {}", e.getMessage());
            throw new RuntimeException("Error creating outbox event", e);
        }
    }

    /**
     * Crea evento de ORDER_PAID para generar tickets.
     * Incluye los items de la orden para que event-service pueda procesarlos.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createOrderPaidEvent(String orderNumber, Long userId, Long adminId, String customerEmail,
                                     List<Map<String, Object>> orderItems) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderNumber", orderNumber);
        payload.put("userId", userId);
        payload.put("adminId", adminId);
        payload.put("customerEmail", customerEmail);
        payload.put("items", orderItems);
        
        createEvent(orderNumber, EventType.ORDER_PAID, payload);
    }

    /**
     * Crea evento de ORDER_CANCELLED para liberar stock
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createOrderCancelledEvent(String orderNumber, Long adminId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderNumber", orderNumber);
        payload.put("adminId", adminId);
        
        createEvent(orderNumber, EventType.ORDER_CANCELLED, payload);
    }
}
