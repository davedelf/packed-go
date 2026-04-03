package com.packed_go.event_service.dtos.qr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateConsumptionQRResponse {
    private Boolean success;
    private String message;
    private ConsumptionRedeemInfo consumptionInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumptionRedeemInfo {
        private Long detailId;
        private Long consumptionId;
        private String consumptionName;
        private String consumptionType;
        private Integer quantityRedeemed;
        private Integer remainingQuantity;
        private Boolean fullyRedeemed;
        private String customerName;  // Opcional
        private String eventName;
    }
}
