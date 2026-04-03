package com.packed_go.order_service.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para eventos recibidos de EVENT-SERVICE
 * Debe coincidir con la estructura de la entidad Event en EVENT-SERVICE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private LocalDateTime eventDate;
    
    private Double lat;
    
    private Double lng;
    
    private Integer maxCapacity;
    
    private BigDecimal basePrice;
    
    private String imageUrl;
    
    private String status; // ACTIVE, CANCELLED, COMPLETED, etc.
    
    private Integer availablePasses;
    
    private Integer totalPasses;
    
    private Long createdBy; // AdminId del organizador del evento
}
