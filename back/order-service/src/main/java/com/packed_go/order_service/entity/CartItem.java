package com.packed_go.order_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private ShoppingCart cart;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "event_name", nullable = false)
    private String eventName;
    
    @Column(name = "admin_id", nullable = false)
    private Long adminId; // ID del admin dueño del evento (para multitenant)

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItemConsumption> consumptions = new ArrayList<>();

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // NO calcular subtotal aquí porque las consumiciones pueden no estar inicializadas
    }

    @PreUpdate
    protected void onUpdate() {
        // NO calcular subtotal aquí automáticamente
        // El servicio debe llamar explícitamente a calculateSubtotal() cuando sea necesario
    }

    // ============================================
    // Business Methods
    // ============================================

    /**
     * Calcula el subtotal del item (precio base + consumiciones)
     */
    public void calculateSubtotal() {
        BigDecimal consumptionsTotal = consumptions.stream()
                .map(CartItemConsumption::getSubtotal)
                .filter(subtotal -> subtotal != null) // Filtrar nulos por seguridad
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                .add(consumptionsTotal);
    }

    /**
     * Agrega una consumición al item
     */
    public void addConsumption(CartItemConsumption consumption) {
        consumptions.add(consumption);
        consumption.setCartItem(this);
        calculateSubtotal();
    }

    /**
     * Elimina una consumición del item
     */
    public void removeConsumption(CartItemConsumption consumption) {
        consumptions.remove(consumption);
        consumption.setCartItem(null);
        calculateSubtotal();
    }

    /**
     * Actualiza la cantidad del item
     */
    public void updateQuantity(Integer newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        this.quantity = newQuantity;
        calculateSubtotal();
    }
}
