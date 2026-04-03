package com.packed_go.order_service.dto.external;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketConsumptionDTO {
    private Long ticketConsumptionId;  // Para compatibilidad
    private Long consumptionId;        // ID de la consumición
    private String consumptionName;    // Nombre de la consumición
    private BigDecimal priceAtPurchase; // Precio al momento de la compra
    private Integer quantity;
}
