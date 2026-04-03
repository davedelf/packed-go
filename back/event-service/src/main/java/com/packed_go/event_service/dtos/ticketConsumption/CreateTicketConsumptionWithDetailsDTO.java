package com.packed_go.event_service.dtos.ticketConsumption;

import com.packed_go.event_service.dtos.ticketConsumptionDetail.CreateTicketConsumptionDetailDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTicketConsumptionWithDetailsDTO {
    private List<CreateTicketConsumptionDetailDTO> details;
}
