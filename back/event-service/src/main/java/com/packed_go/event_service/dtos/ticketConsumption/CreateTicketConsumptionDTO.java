package com.packed_go.event_service.dtos.ticketConsumption;

import com.packed_go.event_service.dtos.consumption.SimpleConsumptionDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTicketConsumptionDTO {
    private List<SimpleConsumptionDTO> consumptions;
}
