package com.packed_go.event_service.dtos.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventDTO {

    private Long id;
    private Long categoryId;
    private EventCategoryDTO category; // Objeto categoría completo
    private String name;
    private String description;
    private LocalDateTime eventDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    //    private Point location;
    private double lat;
    private double lng;
    private String locationName;
    private Integer maxCapacity;
    private Integer currentCapacity;
    private BigDecimal basePrice;
    private String imageUrl;
    private boolean hasImageData;
    private String imageContentType;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;

    // Información de Pass
    private Integer totalPasses;
    private Integer availablePasses;
    private Integer soldPasses;
    
    // Consumptions disponibles (nuevo campo)
    private List<ConsumptionDTO> availableConsumptions;
    
    // IDs de consumptions a asociar/desasociar
    private List<Long> consumptionIds;
}
