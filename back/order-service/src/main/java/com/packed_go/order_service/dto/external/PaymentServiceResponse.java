package com.packed_go.order_service.dto.external;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para recibir respuesta de payment-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentServiceResponse {
    
    private Long paymentId;
    private String orderId;
    private String status;
    private BigDecimal amount;
    private String currency;
    
    // MercadoPago fields (LEGACY)
    private String preferenceId;
    private String initPoint;
    private String sandboxInitPoint;
    
    // Stripe fields (RECOMMENDED)
    private String sessionId;           // Stripe session ID
    private String checkoutUrl;         // Stripe checkout URL
    private String paymentProvider;     // "STRIPE" or "MERCADOPAGO"
    
    private String message;
}
