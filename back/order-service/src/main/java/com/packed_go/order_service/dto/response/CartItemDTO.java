package com.packed_go.order_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta para un item individual del carrito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    
    private Long id;
    
    private Long eventId;
    
    private String eventName;
    
    private Long adminId;
    
    private Integer quantity;
    
    private BigDecimal unitPrice;
    
    private BigDecimal subtotal;
    
    private List<CartItemConsumptionDTO> consumptions;
}
