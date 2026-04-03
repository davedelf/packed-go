package com.packedgo.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutRequest {
    
    private String orderId;
    private String customerEmail;
    private List<ItemDTO> items;
    private String successUrl;
    private String cancelUrl;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private String name;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice; // En dólares
    }
}
