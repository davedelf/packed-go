package com.packed_go.analytics_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tendencia mensual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDTO {
    
    private Integer year;
    private Integer month;
    private String monthName;
    private Long count;
    private BigDecimal amount;
}
