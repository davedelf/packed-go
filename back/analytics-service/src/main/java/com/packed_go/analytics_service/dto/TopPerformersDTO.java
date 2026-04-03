package com.packed_go.analytics_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top performers (mejores eventos, consumiciones, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopPerformersDTO {
    
    // Top 5 eventos más vendidos
    private List<EventPerformanceDTO> topEvents;
    
    // Top 5 consumiciones más vendidas
    private List<ConsumptionPerformanceDTO> topConsumptions;
    
    // Top 5 categorías de eventos más populares
    private List<CategoryPerformanceDTO> topEventCategories;
    
    // Top 5 categorías de consumiciones más populares
    private List<CategoryPerformanceDTO> topConsumptionCategories;
}
