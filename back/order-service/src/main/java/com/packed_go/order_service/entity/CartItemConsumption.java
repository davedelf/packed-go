package com.packed_go.order_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_item_consumptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    @Column(name = "consumption_id", nullable = false)
    private Long consumptionId;

    @Column(name = "consumption_name", nullable = false)
    private String consumptionName;

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
     * Actualiza la cantidad de la consumición
     */
    public void updateQuantity(Integer newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        this.quantity = newQuantity;
        calculateSubtotal();
    }
}
