package com.packed_go.order_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {
    
    @NotBlank(message = "Order number is required")
    private String orderNumber;
    
    @NotNull(message = "Payment status is required")
    private String paymentStatus; // APPROVED, REJECTED, CANCELLED, etc.
    
    private Long mpPaymentId;
    private String paymentMethod;
    private String customerEmail;
}
