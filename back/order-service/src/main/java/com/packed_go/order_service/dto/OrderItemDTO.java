package com.packed_go.order_service.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un item de una orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long eventId;
    private String eventName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    
    @Builder.Default
    private List<ConsumptionDTO> consumptions = new ArrayList<>();
}
