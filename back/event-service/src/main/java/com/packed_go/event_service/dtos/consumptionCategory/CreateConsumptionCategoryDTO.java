package com.packed_go.event_service.dtos.consumptionCategory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConsumptionCategoryDTO {
    private String name;
    private String description;
    private Long createdBy;
}
