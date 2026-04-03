package com.packed_go.event_service.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tickets")
@Getter
@Setter
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referencia al usuario que compró el ticket (viene de otro microservicio)
    @Column(nullable = false)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pass_id", nullable = false, unique = true)
    private Pass pass;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_consumption_id", nullable = false, unique = true)
    private TicketConsumption ticketConsumption;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean redeemed = false;

    // Código QR en formato texto (para validación): PACKEDGO|T:ticketId|TC:consumptionId|E:eventId|U:userId|TS:timestamp
    @Column(columnDefinition = "TEXT")
    private String qrText;

    // Imagen del QR en formato base64 (para escanear)
    @Column(columnDefinition = "TEXT")
    private String qrCode;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime purchasedAt;
    private LocalDateTime redeemedAt;

    @Version
    private Long version;

    // Constructor por defecto
    public Ticket() {}

    // Constructor para crear un ticket
    public Ticket(Long userId, Pass pass, TicketConsumption ticketConsumption) {
        this.userId = userId;
        this.pass = pass;
        this.ticketConsumption = ticketConsumption;
        this.active = true;
        this.redeemed = false;
        this.createdAt = LocalDateTime.now();
        this.purchasedAt = LocalDateTime.now();
    }
    
    // Constructor para crear un ticket con fecha de compra específica
    public Ticket(Long userId, Pass pass, TicketConsumption ticketConsumption, LocalDateTime purchasedAt) {
        this.userId = userId;
        this.pass = pass;
        this.ticketConsumption = ticketConsumption;
        this.active = true;
        this.redeemed = false;
        this.createdAt = LocalDateTime.now();
        this.purchasedAt = purchasedAt != null ? purchasedAt : LocalDateTime.now();
    }
}
