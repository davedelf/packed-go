package com.packed_go.event_service.dtos.ticketConsumptionDetail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedeemTicketDetailDTO {
    private Long ticketDetailId;
    private boolean success;
    private String message;
    private boolean allDetailsRedeemed;
    private boolean ticketRedeemed;
}
