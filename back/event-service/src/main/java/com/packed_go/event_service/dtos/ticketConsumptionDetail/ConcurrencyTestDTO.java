package com.packed_go.event_service.dtos.ticketConsumptionDetail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConcurrencyTestDTO {
    private Long ticketDetailId;
    private int concurrentRequests;
    private int successfulRedemptions;
    private int failedRedemptions;
    private String testResult;
    private long executionTimeMs;
}
