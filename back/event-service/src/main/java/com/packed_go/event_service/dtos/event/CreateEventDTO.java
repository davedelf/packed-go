package com.packed_go.event_service.dtos.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEventDTO {

    private Long categoryId;
    private String name;
    private String description;
    private LocalDateTime eventDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double lat;
    private double lng;
    private String locationName;
    private Integer maxCapacity;
    private Integer currentCapacity;
    private BigDecimal basePrice;
    private String imageUrl;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Lista de IDs de consumiciones disponibles para el evento
    private List<Long> consumptionIds;
}
