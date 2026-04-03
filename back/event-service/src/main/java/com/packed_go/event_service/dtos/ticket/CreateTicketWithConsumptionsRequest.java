package com.packed_go.event_service.dtos.ticket;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO para crear un ticket completo con sus consumiciones.
 * Este endpoint será llamado por consumption-service después de confirmar el pago.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketWithConsumptionsRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @Valid
    private List<ConsumptionItemDTO> consumptions;
    
    private LocalDateTime purchasedAt; // Fecha de compra desde order-service
}
