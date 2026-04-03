package com.packed_go.event_service.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "consumptions")
@Getter
@Setter

public class Consumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ConsumptionCategory category;

    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    
    @Column(columnDefinition = "BYTEA")
    private byte[] imageData;
    
    private String imageContentType;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private boolean active;

    //Propiedades de navegación con otras tablas

    @OneToMany(mappedBy = "consumption", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    private List<TicketConsumptionDetail> ticketDetails = new ArrayList<>();

}
