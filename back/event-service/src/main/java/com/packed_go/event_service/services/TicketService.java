package com.packed_go.event_service.services;

import java.util.List;

import com.packed_go.event_service.dtos.ticket.CreateTicketDTO;
import com.packed_go.event_service.dtos.ticket.CreateTicketWithConsumptionsRequest;
import com.packed_go.event_service.dtos.ticket.TicketDTO;
import com.packed_go.event_service.dtos.ticket.TicketWithConsumptionsResponse;

import jakarta.transaction.Transactional;

public interface TicketService {

    @Transactional
    TicketDTO createTicket(CreateTicketDTO createTicketDTO);

    @Transactional
    TicketDTO purchaseTicket(Long userId, String passCode, Long ticketConsumptionId);

    @Transactional
    TicketWithConsumptionsResponse createTicketWithConsumptions(CreateTicketWithConsumptionsRequest request);

    @Transactional
    TicketDTO redeemTicket(Long ticketId);

    TicketDTO findById(Long id);

    TicketDTO findByPassCode(String passCode);

    List<TicketDTO> findByUserId(Long userId);

    List<TicketDTO> findByUserIdAndActive(Long userId, boolean active);

    List<TicketDTO> findByUserIdAndRedeemed(Long userId, boolean redeemed);

    List<TicketDTO> findByEventId(Long eventId);

    Long countTicketsByEventId(Long eventId);

    Long countRedeemedTicketsByEventId(Long eventId);

    @Transactional
    boolean isTicketRedeemed(Long ticketId);

    java.util.Map<String, Long> getTicketRedemptionStatsByOrganizer(Long organizerId);
}
