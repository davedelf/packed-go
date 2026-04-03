package com.packed_go.event_service.dtos.ticket;

import java.time.LocalDateTime;

import com.packed_go.event_service.dtos.pass.PassDTO;
import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketDTO {
    private Long id;
    private Long userId;
    private PassDTO pass;
    private String passCode; // Código del pase (para mostrar en UI)
    private String qrCode; // QR code en formato base64
    private TicketConsumptionDTO ticketConsumption;
    
    // Event details
    private Long eventId;
    private String eventName;
    private LocalDateTime eventDate;
    private String eventLocation;
    private String eventLocationName; // Nombre del lugar (ej: "Plaza Próspero Molina")
    private Double eventLat; // Latitud para Google Maps
    private Double eventLng; // Longitud para Google Maps
    
    private boolean active;
    private boolean redeemed;
    private LocalDateTime createdAt;
    private LocalDateTime purchasedAt;
    private LocalDateTime redeemedAt;
}
