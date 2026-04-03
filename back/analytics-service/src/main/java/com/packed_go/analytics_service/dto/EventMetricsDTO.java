package com.packed_go.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métricas de eventos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMetricsDTO {
    
    // Total de eventos creados
    private Long totalEvents;
    
    // Eventos activos
    private Long activeEvents;
    
    // Eventos finalizados
    private Long completedEvents;
    
    // Eventos cancelados
    private Long cancelledEvents;
    
    // Eventos programados (futuros)
    private Long upcomingEvents;
    
    // Capacidad total disponible
    private Long totalCapacity;
    
    // Capacidad ocupada
    private Long occupiedCapacity;
    
    // Tasa de ocupación promedio (%)
    private Double averageOccupancyRate;
    
    // Evento más vendido (ID)
    private Long mostSoldEventId;
    
    // Evento más vendido (nombre)
    private String mostSoldEventName;
    
    // Tickets vendidos del evento más popular
    private Long mostSoldEventTickets;
}
