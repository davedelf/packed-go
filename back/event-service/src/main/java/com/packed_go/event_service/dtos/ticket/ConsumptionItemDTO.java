package com.packed_go.event_service.dtos.ticket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO que representa un item de consumición para incluir en un ticket.
 * Usado en CreateTicketWithConsumptionsRequest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsumptionItemDTO {

    @NotNull(message = "Consumption ID is required")
    private Long consumptionId;

    @NotBlank(message = "Consumption name is required")
    private String consumptionName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price at purchase is required")
    @Min(value = 0, message = "Price must be positive")
    private BigDecimal priceAtPurchase;
}
