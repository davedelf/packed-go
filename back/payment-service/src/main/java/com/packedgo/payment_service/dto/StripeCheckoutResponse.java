package com.packedgo.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutResponse {
    private String sessionId;
    private String checkoutUrl;
    private String status;
    private String paymentIntentId;
}
