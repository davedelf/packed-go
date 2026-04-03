package com.packedgo.payment_service.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id"),
        @Index(name = "idx_payments_stripe_session_id", columnList = "stripe_session_id"),
        @Index(name = "idx_payments_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "order_id", nullable = false, unique = true, length = 255)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @lombok.Builder.Default
    private String currency = "ARS";

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Column(name = "payer_email", length = 255)
    private String payerEmail;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "description", length = 500)
    private String description;

    // Stripe fields
    @Column(name = "stripe_session_id", length = 255)
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "payment_provider", length = 50)
    @lombok.Builder.Default
    private String paymentProvider = "STRIPE"; // Only STRIPE

    @Column(name = "transaction_amount", precision = 10, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "status_detail", length = 100)
    private String statusDetail;

    @Column(name = "payment_type_id", length = 50)
    private String paymentTypeId;

    @Column(name = "installments")
    private Integer installments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (currency == null) {
            currency = "ARS";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == PaymentStatus.APPROVED && approvedAt == null) {
            approvedAt = LocalDateTime.now();
        }
    }

    /**
     * Estados posibles de un pago según MercadoPago
     */
    public enum PaymentStatus {
        PENDING("Pendiente"),
        APPROVED("Aprobado"),
        REJECTED("Rechazado"),
        CANCELLED("Cancelado"),
        REFUNDED("Reembolsado"),
        IN_PROCESS("En proceso"),
        IN_MEDIATION("En mediación"),
        CHARGED_BACK("Contracargo");

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Verifica si el pago fue exitoso
     */
    public boolean isSuccessful() {
        return status == PaymentStatus.APPROVED;
    }

    /**
     * Verifica si el pago está pendiente
     */
    public boolean isPending() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.IN_PROCESS;
    }

    /**
     * Verifica si el pago falló
     */
    public boolean isFailed() {
        return status == PaymentStatus.REJECTED || status == PaymentStatus.CANCELLED;
    }
}
