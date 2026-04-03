package com.packedgo.payment_service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsDTO {
    private Long totalPayments;
    private Long approvedPayments;
    private Long pendingPayments;
    private Long rejectedPayments;
    private BigDecimal totalRevenue;
    private BigDecimal approvedRevenue;
    private BigDecimal pendingRevenue;
    private Double approvalRate;
}
