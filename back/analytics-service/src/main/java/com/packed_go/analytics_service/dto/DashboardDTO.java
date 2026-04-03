package com.packed_go.analytics_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el Dashboard completo de Analytics
 * Contiene todas las métricas y estadísticas relevantes para un organizador
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    
    // === MÉTRICAS GENERALES ===
    private Long organizerId;
    private String organizerName;
    private LocalDateTime lastUpdated;
    
    // === VENTAS ===
    private SalesMetricsDTO salesMetrics;
    
    // === EVENTOS ===
    private EventMetricsDTO eventMetrics;
    
    // === CONSUMICIONES ===
    private ConsumptionMetricsDTO consumptionMetrics;
    
    // === INGRESOS ===
    private RevenueMetricsDTO revenueMetrics;
    
    // === TOP PERFORMERS ===
    private TopPerformersDTO topPerformers;
    
    // === TENDENCIAS ===
    private TrendsDTO trends;
}
