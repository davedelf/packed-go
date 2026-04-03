package com.packed_go.order_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para un consumo dentro de un item del carrito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemConsumptionDTO {
    
    private Long id;
    
    private Long consumptionId;
    
    private String consumptionName;
    
    private Integer quantity;
    
    private BigDecimal unitPrice;
    
    private BigDecimal subtotal;
}
