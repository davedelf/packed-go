package com.packed_go.event_service.dtos.consumption;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsumptionDTO {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String imageData; // Base64-encoded image
    private String imageContentType;
    private boolean hasImageData; // Flag para indicar si tiene imagen local
    private Long createdBy;
    private boolean active;


}