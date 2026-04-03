package com.packed_go.event_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para recibir eventos de ORDER_PAID desde RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEventDTO {

    private String orderNumber;
    private Long userId;
    private Long adminId;
    private String customerEmail;
    
    // Items de la orden para generar tickets
    private List<OrderItemDTO> items;
    
    /**
     * DTO para cada item de la orden
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Long eventId;
        private Integer quantity;
        private List<ConsumptionItemDTO> consumptions;
    }
    
    /**
     * DTO para consumiciones en cada item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumptionItemDTO {
        private Long consumptionId;
        private String consumptionName;
        private java.math.BigDecimal priceAtPurchase;
        private Integer quantity;
    }
}
