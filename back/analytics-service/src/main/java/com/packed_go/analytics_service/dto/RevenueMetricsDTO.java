package com.packed_go.analytics_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métricas de ingresos financieros
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueMetricsDTO {
    
    // Ingresos totales
    private BigDecimal totalRevenue;
    
    // Ingresos hoy
    private BigDecimal revenueToday;
    
    // Ingresos esta semana
    private BigDecimal revenueThisWeek;
    
    // Ingresos este mes
    private BigDecimal revenueThisMonth;
    
    // Ingresos por entradas
    private BigDecimal revenueFromTickets;
    
    // Ingresos por consumiciones
    private BigDecimal revenueFromConsumptions;
    
    // Ingreso promedio por evento
    private BigDecimal averageRevenuePerEvent;
    
    // Ingreso promedio por cliente
    private BigDecimal averageRevenuePerCustomer;
}
