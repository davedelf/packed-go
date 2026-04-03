package com.packed_go.order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para agregar un evento al carrito
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    
    @NotNull(message = "Event ID is required")
    @Positive(message = "Event ID must be positive")
    private Long eventId;
    
    /**
     * Cantidad de entradas del evento a agregar
     * Si no se especifica, se asume 1
     */
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
    
    /**
     * Lista de consumos a agregar con el evento
     * Puede estar vacía si solo se quiere agregar el evento base
     */
    @Valid
    private List<ConsumptionRequest> consumptions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumptionRequest {
        
        @NotNull(message = "Consumption ID is required")
        @Positive(message = "Consumption ID must be positive")
        private Long consumptionId;
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;
    }
}
