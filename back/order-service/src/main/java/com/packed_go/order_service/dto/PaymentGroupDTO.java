package com.packed_go.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un grupo de pago por admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGroupDTO {
    private Long adminId;
    private String adminName; // Nombre del admin/organizador
    private String orderNumber;
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private String paymentPreferenceId;
    private String qrUrl;
    private String initPoint;
    private List<OrderItemDTO> items;
}
