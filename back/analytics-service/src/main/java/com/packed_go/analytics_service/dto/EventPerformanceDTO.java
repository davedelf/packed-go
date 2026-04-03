package com.packed_go.analytics_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Performance de un evento específico
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPerformanceDTO {
    
    private Long eventId;
    private String eventName;
    private Long ticketsSold;
    private BigDecimal revenue;
    private Double occupancyRate;
}
