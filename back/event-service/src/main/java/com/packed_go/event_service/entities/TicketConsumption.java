package com.packed_go.event_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consumption_tickets")
@Getter
@Setter
public class TicketConsumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime createdAt= LocalDateTime.now();
    private boolean active=true;
    private boolean redeem=false;
    
    @Version
    private Long version;
    
    @OneToMany(mappedBy = "ticketConsumption", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<TicketConsumptionDetail> consumptionDetails = new ArrayList<>();

}
