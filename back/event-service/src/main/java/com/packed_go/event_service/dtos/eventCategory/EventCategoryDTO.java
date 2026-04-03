package com.packed_go.event_service.dtos.eventCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventCategoryDTO {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private Long createdBy;
}
