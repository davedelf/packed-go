package com.packed_go.event_service.dtos.ticket;

import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionWithDetailsDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTicketDTO {
    private Long userId;
    private String passCode;
    private CreateTicketConsumptionWithDetailsDTO ticketConsumption;
}
