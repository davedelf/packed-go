package com.packed_go.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métricas de consumiciones
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionMetricsDTO {
    
    // Total de consumiciones creadas
    private Long totalConsumptions;
    
    // Consumiciones activas
    private Long activeConsumptions;
    
    // Total de consumiciones vendidas (asociadas a tickets)
    private Long totalConsumptionsSold;
    
    // Consumiciones canjeadas
    private Long consumptionsRedeemed;
    
    // Consumiciones pendientes de canje
    private Long consumptionsPending;
    
    // Tasa de canje de consumibles (%)
    private Double redemptionRate;
    
    // === MÉTRICAS DE ENTRADAS ===
    
    // Total de entradas vendidas
    private Long totalTicketsSold;
    
    // Entradas canjeadas
    private Long ticketsRedeemed;
    
    // Entradas pendientes de canje
    private Long ticketsPending;
    
    // Tasa de canje de entradas (%)
    private Double ticketRedemptionRate;
    
    // Consumición más vendida (ID)
    private Long mostSoldConsumptionId;
    
    // Consumición más vendida (nombre)
    private String mostSoldConsumptionName;
    
    // Cantidad vendida de la consumición más popular
    private Long mostSoldConsumptionQuantity;
}
