package com.packed_go.order_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    
    @NotNull(message = "Admin ID is required")
    private Long adminId;
    
    private String timezone; // Timezone del cliente (ej: "America/Argentina/Buenos_Aires")
    
    private String successUrl;
    private String failureUrl;
    private String pendingUrl;
}
