package com.packed_go.event_service.dtos.consumption;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateConsumptionDTO {


    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String imageData; // Base64-encoded image
    private String imageContentType;
    
    // Este campo NO se envía desde frontend, se inyecta desde JWT en el controller
    private Long createdBy;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private boolean active=true;

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
