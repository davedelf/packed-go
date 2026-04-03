package com.packed_go.analytics_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Performance de una consumición específica
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionPerformanceDTO {
    
    private Long consumptionId;
    private String consumptionName;
    private Long quantitySold;
    private BigDecimal revenue;
    private Double redemptionRate;
}
