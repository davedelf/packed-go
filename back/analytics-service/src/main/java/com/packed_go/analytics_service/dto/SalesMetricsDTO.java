package com.packed_go.analytics_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métricas de ventas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesMetricsDTO {
    
    // Total de tickets vendidos
    private Long totalTicketsSold;
    
    // Tickets vendidos hoy
    private Long ticketsSoldToday;
    
    // Tickets vendidos esta semana
    private Long ticketsSoldThisWeek;
    
    // Tickets vendidos este mes
    private Long ticketsSoldThisMonth;
    
    // Total de órdenes
    private Long totalOrders;
    
    // Órdenes pagadas
    private Long paidOrders;
    
    // Órdenes pendientes
    private Long pendingOrders;
    
    // Órdenes canceladas
    private Long cancelledOrders;
    
    // Tasa de conversión (%)
    private Double conversionRate;
    
    // Ticket promedio (valor medio de venta)
    private BigDecimal averageOrderValue;
    
    // Promedio de tickets por orden
    private Double averageTicketsPerOrder;
}
