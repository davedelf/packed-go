package com.packed_go.event_service.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="category_id")
    private EventCategory category;

    private String name;
    private String description;
    private LocalDateTime eventDate;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    private Double lat;
    private Double lng;
    
    @Column(name = "location_name")
    private String locationName;
    
    private Integer maxCapacity;
    private Integer currentCapacity;
    private BigDecimal basePrice;
    private String imageUrl;
    
    @Column(columnDefinition = "BYTEA")
    private byte[] imageData;
    
    private String imageContentType;
    
    @Column(nullable = false, length = 20)
    private String status;
    
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private boolean active;

    // Nueva funcionalidad: Gestión de Pass
    @Column(nullable = false)
    private Integer totalPasses;

    @Column(nullable = false)
    private Integer availablePasses;

    @Column(nullable = false)
    private Integer soldPasses;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pass> passes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "event_consumptions",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "consumption_id")
    )
    private List<Consumption> consumptions = new ArrayList<>();

    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long version = 0L;

    // Constructor por defecto
    public Event() {
        this.status = "ACTIVE";
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.totalPasses = 0;
        this.availablePasses = 0;
        this.soldPasses = 0;
        this.currentCapacity = 0;
        this.passes = new ArrayList<>();
        this.consumptions = new ArrayList<>();
        this.version = 0L;
    }

    // Métodos de utilidad para gestión de Pass
    public void addPass(Pass pass) {
        this.passes.add(pass);
        this.totalPasses++;
        this.availablePasses++;
    }

    public void markPassAsSold() {
        this.soldPasses++;
        this.availablePasses--;
    }

    public boolean hasAvailablePasses() {
        return this.availablePasses > 0;
    }

}
