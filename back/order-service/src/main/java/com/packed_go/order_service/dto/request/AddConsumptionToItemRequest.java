package com.packed_go.order_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para agregar una consumición a un item existente del carrito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddConsumptionToItemRequest {
    
    @NotNull(message = "Consumption ID is required")
    @Positive(message = "Consumption ID must be positive")
    private Long consumptionId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}
