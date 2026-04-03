package com.packed_go.event_service.dtos.pass;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PassDTO {
    private Long id;
    private String code;
    private Long eventId;
    private boolean active;
    private boolean available;
    private boolean sold;
    private LocalDateTime createdAt;
    private LocalDateTime soldAt;
    private Long soldToUserId;
}
