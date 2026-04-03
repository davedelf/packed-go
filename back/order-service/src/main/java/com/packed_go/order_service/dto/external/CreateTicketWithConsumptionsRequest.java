package com.packed_go.order_service.dto.external;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketWithConsumptionsRequest {
    private Long userId;
    private Long eventId;
    private List<TicketConsumptionDTO> consumptions;
    private LocalDateTime purchasedAt; // Fecha de compra desde la orden
}
