package com.packed_go.event_service.dtos.ticketConsumption;

import com.packed_go.event_service.dtos.ticketConsumptionDetail.CreateTicketConsumptionDetailSimpleDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTicketConsumptionWithSimpleDetailsDTO {
    private List<CreateTicketConsumptionDetailSimpleDTO> details;
}
