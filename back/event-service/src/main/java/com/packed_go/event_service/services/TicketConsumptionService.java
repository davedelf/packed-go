package com.packed_go.event_service.services;

import java.util.List;

import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionWithDetailsDTO;
import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionWithSimpleDetailsDTO;
import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.entities.TicketConsumption;

import jakarta.transaction.Transactional;

public interface TicketConsumptionService {


    @Transactional
    TicketConsumptionDTO create(TicketConsumptionDTO ticketConsumptionDTO);

    @Transactional
    TicketConsumptionDTO createFromConsumptions(CreateTicketConsumptionDTO createTicketConsumptionDTO);

    @Transactional
    TicketConsumptionDTO createWithDetails(CreateTicketConsumptionWithDetailsDTO createTicketConsumptionWithDetailsDTO);

    @Transactional
    TicketConsumptionDTO createWithSimpleDetails(CreateTicketConsumptionWithSimpleDetailsDTO createTicketConsumptionWithSimpleDetailsDTO);

    @Transactional
    TicketConsumption update(Long id, TicketConsumptionDTO ticketConsumptionDTO);

    @Transactional
    TicketConsumption deleteLogical(Long id);

    boolean redeemTicketDetails();


    List<TicketConsumptionDetailDTO> getTicketConsumptionDetails(Long id);

    TicketConsumptionDTO findById(Long id);

    java.util.Map<String, Long> getRedemptionStatsByOrganizer(Long organizerId);

}
