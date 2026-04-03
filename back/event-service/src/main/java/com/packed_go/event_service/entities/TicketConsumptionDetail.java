package com.packed_go.event_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Entity
@Table(name = "ticket_consumption_details")
@Getter
@Setter
public class TicketConsumptionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // Un detalle pertenece a un consumo específico
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumption_id", nullable = false)
    private Consumption consumption;

    // Un detalle pertenece a un ticketConsumption
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumption_ticket_id", nullable = false)
    private TicketConsumption ticketConsumption;

    private Integer quantity;

    // Precio histórico en el momento de la compra
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    private boolean active = true;
    private boolean redeem = false;

    @Version
    private Long version;

}
