package com.packed_go.order_service.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {
    
    private Long orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String status;
    
    // URLs de MercadoPago
    private String paymentUrl;      // initPoint o sandboxInitPoint
    private String preferenceId;    // ID de preferencia de MercadoPago
    
    private String message;
}
