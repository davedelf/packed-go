package com.packed_go.event_service.dtos.ticketConsumptionDetail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTicketConsumptionDetailSimpleDTO {
    private Long consumptionId;
    private Integer quantity;
}
