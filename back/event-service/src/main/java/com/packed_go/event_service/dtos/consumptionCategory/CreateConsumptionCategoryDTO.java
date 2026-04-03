package com.packed_go.event_service.dtos.consumptionCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateConsumptionCategoryDTO {
    private String name;
    private String description;
    private Long createdBy;

    public String getName() {
        return name;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
