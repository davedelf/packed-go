package com.packed_go.event_service.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.event_service.dtos.event.EventDTO;
import com.packed_go.event_service.dtos.ticket.CreateTicketDTO;
import com.packed_go.event_service.dtos.ticket.CreateTicketWithConsumptionsRequest;
import com.packed_go.event_service.dtos.ticket.TicketDTO;
import com.packed_go.event_service.dtos.ticket.TicketWithConsumptionsResponse;
import com.packed_go.event_service.security.JwtTokenValidator;
import com.packed_go.event_service.services.EventService;
import com.packed_go.event_service.services.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/event-service/tickets")
@RequiredArgsConstructor
public class TicketController {

    private static final Logger log = LoggerFactory.getLogger(TicketController.class);

    private final TicketService ticketService;
    private final EventService eventService;
    private final JwtTokenValidator jwtValidator;

    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(@RequestBody CreateTicketDTO createTicketDTO) {
        log.info("Creando ticket para usuario: {} con pass: {}", createTicketDTO.getUserId(), createTicketDTO.getPassCode());
        TicketDTO ticket = ticketService.createTicket(createTicketDTO);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/create-with-consumptions")
    public ResponseEntity<TicketWithConsumptionsResponse> createTicketWithConsumptions(
            @Valid @RequestBody CreateTicketWithConsumptionsRequest request) {
        log.info("Creando ticket con consumiciones para usuario: {}, evento: {}", 
                request.getUserId(), request.getEventId());
        TicketWithConsumptionsResponse response = ticketService.createTicketWithConsumptions(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/purchase")
    public ResponseEntity<TicketDTO> purchaseTicket(@RequestParam Long userId, @RequestParam String passCode, @RequestParam Long ticketConsumptionId) {
        log.info("Comprando ticket para usuario: {} con pass: {}", userId, passCode);
        TicketDTO ticket = ticketService.purchaseTicket(userId, passCode, ticketConsumptionId);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/{ticketId}/redeem")
    public ResponseEntity<TicketDTO> redeemTicket(@PathVariable Long ticketId) {
        log.info("Canjeando ticket: {}", ticketId);
        TicketDTO ticket = ticketService.redeemTicket(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketDTO> getTicket(@PathVariable Long ticketId) {
        log.info("Obteniendo ticket: {}", ticketId);
        TicketDTO ticket = ticketService.findById(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/pass-code/{passCode}")
    public ResponseEntity<TicketDTO> getTicketByPassCode(@PathVariable String passCode) {
        log.info("Obteniendo ticket con código de pass: {}", passCode);
        TicketDTO ticket = ticketService.findByPassCode(passCode);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketDTO>> getTicketsByUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId) {
        log.info("Obteniendo tickets para usuario: {}", userId);
        validateAndExtractUserId(authHeader, userId);
        List<TicketDTO> tickets = ticketService.findByUserId(userId);
        
        // Log para debug
        tickets.forEach(ticket -> {
            log.info("Ticket ID: {}, Pass Code: {}, QR Code present: {}", 
                    ticket.getId(), 
                    ticket.getPassCode(), 
                    ticket.getQrCode() != null && !ticket.getQrCode().isEmpty());
        });
        
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<TicketDTO>> getActiveTicketsByUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId) {
        log.info("Obteniendo tickets activos para usuario: {}", userId);
        validateAndExtractUserId(authHeader, userId);
        List<TicketDTO> tickets = ticketService.findByUserIdAndActive(userId, true);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/user/{userId}/redeemed")
    public ResponseEntity<List<TicketDTO>> getRedeemedTicketsByUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId) {
        log.info("Obteniendo tickets canjeados para usuario: {}", userId);
        validateAndExtractUserId(authHeader, userId);
        List<TicketDTO> tickets = ticketService.findByUserIdAndRedeemed(userId, true);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/user/{userId}/not-redeemed")
    public ResponseEntity<List<TicketDTO>> getNotRedeemedTicketsByUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId) {
        log.info("Obteniendo tickets no canjeados para usuario: {}", userId);
        validateAndExtractUserId(authHeader, userId);
        List<TicketDTO> tickets = ticketService.findByUserIdAndRedeemed(userId, false);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketDTO>> getTicketsByEvent(@PathVariable Long eventId) {
        log.info("Obteniendo tickets para evento: {}", eventId);
        List<TicketDTO> tickets = ticketService.findByEventId(eventId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/event/{eventId}/count")
    public ResponseEntity<Long> getTicketsCountByEvent(@PathVariable Long eventId) {
        log.info("Contando tickets para evento: {}", eventId);
        Long count = ticketService.countTicketsByEventId(eventId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/event/{eventId}/redeemed/count")
    public ResponseEntity<Long> getRedeemedTicketsCountByEvent(@PathVariable Long eventId) {
        log.info("Contando tickets canjeados para evento: {}", eventId);
        Long count = ticketService.countRedeemedTicketsByEventId(eventId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{ticketId}/is-redeemed")
    public ResponseEntity<Boolean> isTicketRedeemed(@PathVariable Long ticketId) {
        log.info("Verificando si el ticket está canjeado: {}", ticketId);
        boolean isRedeemed = ticketService.isTicketRedeemed(ticketId);
        return ResponseEntity.ok(isRedeemed);
    }

    @GetMapping("/{ticketId}/full")
    public ResponseEntity<TicketWithConsumptionsResponse> getTicketWithConsumptions(@PathVariable Long ticketId) {
        log.info("Obteniendo ticket completo con consumiciones: {}", ticketId);
        TicketDTO ticketDTO = ticketService.findById(ticketId);
        EventDTO eventDTO = eventService.findById(ticketDTO.getPass().getEventId());

        TicketWithConsumptionsResponse response = TicketWithConsumptionsResponse.builder()
                .ticketId(ticketDTO.getId())
                .userId(ticketDTO.getUserId())
                .passCode(ticketDTO.getPass().getCode())
                .passId(ticketDTO.getPass().getId())
                .eventId(eventDTO.getId())
                .eventName(eventDTO.getName())
                .eventDate(eventDTO.getEventDate())
                .active(ticketDTO.isActive())
                .redeemed(ticketDTO.isRedeemed())
                .createdAt(ticketDTO.getCreatedAt())
                .purchasedAt(ticketDTO.getPurchasedAt())
                .redeemedAt(ticketDTO.getRedeemedAt())
                .ticketConsumption(ticketDTO.getTicketConsumption())
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener estadísticas de redención de entradas por organizador.
     * Retorna total de entradas vendidas y total de entradas canjeadas.
     */
    @GetMapping("/redemption-stats/organizer/{organizerId}")
    public ResponseEntity<java.util.Map<String, Long>> getTicketRedemptionStatsByOrganizer(@PathVariable Long organizerId) {
        log.info("📊 Obteniendo estadísticas de redención de ENTRADAS para organizador: {}", organizerId);
        java.util.Map<String, Long> stats = ticketService.getTicketRedemptionStatsByOrganizer(organizerId);
        return ResponseEntity.ok(stats);
    }

    private Long validateAndExtractUserId(String authHeader, Long requestedUserId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }
        
        Long tokenUserId = jwtValidator.getUserIdFromToken(token);
        if (!tokenUserId.equals(requestedUserId)) {
            throw new RuntimeException("Cannot access other user's resources");
        }
        
        return tokenUserId;
    }
}