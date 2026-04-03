package com.packed_go.event_service.dtos.qr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindTicketByCodeResponse {
    private Long ticketId;
    private String qrCode;
    private String passCode;
    private Long eventId;
    private String eventName;
    private Long userId;
}
