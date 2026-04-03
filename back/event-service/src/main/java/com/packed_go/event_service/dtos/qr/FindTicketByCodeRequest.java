package com.packed_go.event_service.dtos.qr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindTicketByCodeRequest {
    private String code;
    private Long eventId;
}
