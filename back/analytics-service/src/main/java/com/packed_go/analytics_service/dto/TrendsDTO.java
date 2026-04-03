package com.packed_go.analytics_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tendencias y gráficos temporales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendsDTO {
    
    // Ventas por día (últimos 30 días)
    private List<DailyTrendDTO> dailySales;
    
    // Ingresos por día (últimos 30 días)
    private List<DailyTrendDTO> dailyRevenue;
    
    // Ventas por mes (último año)
    private List<MonthlyTrendDTO> monthlySales;
    
    // Ingresos por mes (último año)
    private List<MonthlyTrendDTO> monthlyRevenue;
}
