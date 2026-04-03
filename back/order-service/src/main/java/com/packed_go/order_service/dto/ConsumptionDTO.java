package com.packed_go.order_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar una consumición de un item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionDTO {
    private Long consumptionId;
    private String name;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
}
