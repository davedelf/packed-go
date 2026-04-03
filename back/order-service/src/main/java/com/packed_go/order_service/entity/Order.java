package com.packed_go.order_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.packed_go.order_service.util.DateTimeUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_number", columnList = "order_number"),
    @Index(name = "idx_order_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cart_id")
    private Long cartId; // Referencia al carrito original

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "payment_id")
    private Long paymentId; // ID del pago en payment-service

    @Column(name = "payment_preference_id", length = 100)
    private String paymentPreferenceId; // ID de preferencia de MercadoPago

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl; // URL de pago de Stripe

    @Column(name = "session_id", length = 100)
    private String sessionId; // ID de sesión multi-orden

    @Column(name = "admin_id")
    private Long adminId; // Admin del evento (para payment-service)

    @Column(name = "client_timezone", length = 100)
    private String clientTimezone; // Timezone del cliente (ej: "America/Argentina/Buenos_Aires")

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    // ============================================
    // Enums
    // ============================================

    public enum OrderStatus {
        PENDING_PAYMENT,  // Orden creada, esperando pago
        PAID,             // Pago confirmado
        PROCESSING,       // En proceso de entrega
        COMPLETED,        // Completada
        CANCELLED,        // Cancelada
        REFUNDED         // Reembolsada
    }

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        createdAt = clientTimezone != null ? DateTimeUtils.now(clientTimezone) : DateTimeUtils.now();
        updatedAt = clientTimezone != null ? DateTimeUtils.now(clientTimezone) : DateTimeUtils.now();
        
        if (orderNumber == null) {
            orderNumber = generateOrderNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = clientTimezone != null ? DateTimeUtils.now(clientTimezone) : DateTimeUtils.now();
    }

    // ============================================
    // Business Methods
    // ============================================

    /**
     * Genera un número de orden único
     */
    public static String generateOrderNumber() {
        LocalDateTime now = DateTimeUtils.now();
        return "ORD-" + now.getYear() + 
               String.format("%02d", now.getMonthValue()) + "-" +
               System.currentTimeMillis();
    }

    /**
     * Agrega un item a la orden
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Marca la orden como pagada
     */
    public void markAsPaid() {
        this.status = OrderStatus.PAID;
        this.paidAt = clientTimezone != null ? DateTimeUtils.now(clientTimezone) : DateTimeUtils.now();
    }

    /**
     * Marca la orden como cancelada
     */
    public void markAsCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = clientTimezone != null ? DateTimeUtils.now(clientTimezone) : DateTimeUtils.now();
    }

    /**
     * Verifica si la orden está pendiente de pago
     */
    public boolean isPendingPayment() {
        return OrderStatus.PENDING_PAYMENT.equals(this.status);
    }

    /**
     * Verifica si la orden fue pagada
     */
    public boolean isPaid() {
        return OrderStatus.PAID.equals(this.status);
    }

    /**
     * Calcula el total de items
     */
    public int getTotalItems() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
}
