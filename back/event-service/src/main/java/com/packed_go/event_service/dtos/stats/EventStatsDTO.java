package com.packed_go.event_service.dtos.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatsDTO {
    private Long totalEvents;
    private Long activeEvents;
    private Long totalTicketsSold;
    private Long totalCapacity;
    private Long availableCapacity;
    private Double occupancyRate;
    private Long upcomingEvents;
    private Long pastEvents;
}
