package com.packed_go.event_service.dtos.qr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateEntryQRResponse {
    private Boolean valid;
    private String message;
    private TicketEntryInfo ticketInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketEntryInfo {
        private Long ticketId;
        private Long userId;
        private String customerName;  // Opcional - si tienes integración con users
        private String eventName;
        private String passType;
        private Boolean alreadyUsed;
    }
}
