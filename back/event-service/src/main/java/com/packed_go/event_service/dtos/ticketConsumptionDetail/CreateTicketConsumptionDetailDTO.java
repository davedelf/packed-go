package com.packed_go.event_service.dtos.ticketConsumptionDetail;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTicketConsumptionDetailDTO {
    private ConsumptionDTO consumption;
    private Integer quantity;
}
