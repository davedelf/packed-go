package com.packed_go.order_service.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item_consumptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private OrderItem orderItem;

    @Column(name = "consumption_id", nullable = false)
    private Long consumptionId;

    @Column(name = "consumption_name", nullable = false, length = 200)
    private String consumptionName;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    @PreUpdate
    protected void calculateSubtotal() {
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ============================================
    // Business Methods
    // ============================================

    /**
     * Crea un OrderItemConsumption desde un CartItemConsumption
     */
    public static OrderItemConsumption fromCartItemConsumption(CartItemConsumption cartConsumption) {
        return OrderItemConsumption.builder()
                .consumptionId(cartConsumption.getConsumptionId())
                .consumptionName(cartConsumption.getConsumptionName())
                .quantity(cartConsumption.getQuantity())
                .unitPrice(cartConsumption.getUnitPrice())
                .subtotal(cartConsumption.getSubtotal())
                .build();
    }
}
