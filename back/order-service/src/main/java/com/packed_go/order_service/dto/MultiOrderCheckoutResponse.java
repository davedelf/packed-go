package com.packed_go.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta del checkout multitenant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiOrderCheckoutResponse {
    private String sessionId;
    private String sessionToken;
    private BigDecimal totalAmount;
    private String sessionStatus;
    private LocalDateTime expiresAt;
    private Integer totalOrders;
    private Integer paidOrders;
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
    private List<PaymentGroupDTO> paymentGroups;
    private String message;
}
