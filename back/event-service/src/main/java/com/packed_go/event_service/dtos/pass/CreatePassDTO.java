package com.packed_go.event_service.dtos.pass;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePassDTO {
    private Long eventId;
    private String code;
}
