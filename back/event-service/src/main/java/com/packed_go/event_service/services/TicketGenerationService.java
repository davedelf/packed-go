package com.packed_go.event_service.services;

import com.packed_go.event_service.dto.OrderPaidEventDTO;

/**
 * Interfaz para el servicio de generación de tickets.
 */
public interface TicketGenerationService {
    
    /**
     * Genera tickets para una orden pagada.
     * Este método es llamado por el listener de RabbitMQ.
     * 
     * @param event El evento con los datos de la orden pagada
     */
    void generateTicketsForOrder(OrderPaidEventDTO event);
}
