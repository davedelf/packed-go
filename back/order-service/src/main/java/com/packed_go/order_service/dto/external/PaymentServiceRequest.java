package com.packed_go.order_service.dto.external;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear un pago en payment-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentServiceRequest {
    
    private Long adminId;
    private String orderId;
    private BigDecimal amount;
    private String description;
    private String payerEmail;
    private String payerName;
    private String successUrl;
    private String failureUrl;
    private String pendingUrl;
}
