package com.packed_go.event_service.dtos.eventCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEventCategoryDTO {
    private String name;
    private String description;
    private Boolean active;
    private Long createdBy;
}
