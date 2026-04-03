package com.packedgo.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long paymentId;
    private String orderId;
    private String status;
    private BigDecimal amount;
    private String currency;
    
    // MercadoPago fields (LEGACY)
    private String initPoint; // URL de MercadoPago para realizar el pago
    private String preferenceId;
    private String sandboxInitPoint;
    
    // Stripe fields (RECOMMENDED)
    private String sessionId;           // Stripe session ID
    private String checkoutUrl;         // Stripe checkout URL
    private String paymentProvider;     // "STRIPE" or "MERCADOPAGO"
    private String createdAt;
    
    private String message;
}