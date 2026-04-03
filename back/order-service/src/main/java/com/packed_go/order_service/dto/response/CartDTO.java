package com.packed_go.order_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para el carrito de compras completo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    
    private Long id;
    
    private Long userId;
    
    private String status;
    
    private LocalDateTime expiresAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private List<CartItemDTO> items;
    
    private BigDecimal totalAmount;
    
    private Integer itemCount;
    
    private Boolean expired;
}
