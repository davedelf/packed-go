package com.packedgo.payment_service.dto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

// DTO para crear un pago
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "El ID del admin es requerido")
    private Long adminId;

    @NotBlank(message = "El ID de la orden es requerido")
    private String orderId;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotBlank(message = "La descripción es requerida")
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;

    @Email(message = "Email del pagador inválido")
    private String payerEmail;

    private String payerName;

    private String externalReference;

    // URLs de retorno
    @NotBlank(message = "URL de éxito es requerida")
    private String successUrl;

    @NotBlank(message = "URL de fallo es requerida")
    private String failureUrl;

    @NotBlank(message = "URL pendiente es requerida")
    private String pendingUrl;

    // Metadata adicional
    private Map<String, Object> metadata;
}