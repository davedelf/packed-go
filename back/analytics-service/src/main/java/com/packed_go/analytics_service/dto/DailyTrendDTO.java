package com.packed_go.analytics_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tendencia diaria
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendDTO {
    
    private LocalDate date;
    private Long count;
    private BigDecimal amount;
}
