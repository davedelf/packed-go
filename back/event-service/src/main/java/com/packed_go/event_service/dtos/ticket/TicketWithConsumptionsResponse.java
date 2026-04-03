package com.packed_go.event_service.dtos.ticket;

import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO que contiene toda la información del ticket creado
 * con sus consumiciones, para ser usado por consumption-service
 * en la generación de QRs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketWithConsumptionsResponse {

    private Long ticketId;
    private Long userId;
    private String passCode;
    private Long passId;
    
    // QR Code for ticket validation
    private String qrCode;
    
    // Información del evento
    private Long eventId;
    private String eventName;
    private LocalDateTime eventDate;
    private String eventLocation;
    
    // Estado del ticket
    private boolean active;
    private boolean redeemed;
    private LocalDateTime createdAt;
    private LocalDateTime purchasedAt;
    private LocalDateTime redeemedAt;
    
    // Consumiciones incluidas
    private TicketConsumptionDTO ticketConsumption;
}
