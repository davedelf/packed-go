package com.packed_go.order_service.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para consumos recibidos de EVENT-SERVICE
 * Debe coincidir con la estructura de la entidad Consumption en EVENT-SERVICE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionDTO {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private BigDecimal price;
    
    private String imageUrl;
    
    private Long categoryId;
    
    private String categoryName; // Opcional, puede venir en algunas respuestas
    
    private Long createdBy; // 🔒 Nuevo campo multi-tenant (no se usa en ORDER-SERVICE por ahora)
}
