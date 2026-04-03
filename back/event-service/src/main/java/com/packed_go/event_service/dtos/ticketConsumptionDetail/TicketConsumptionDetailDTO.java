package com.packed_go.event_service.dtos.ticketConsumptionDetail;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TicketConsumptionDetailDTO {
    private Long id;
    private Long ticketId;
    private Long consumptionId;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private boolean active;
    private boolean redeem;
    private String consumptionName;
}
