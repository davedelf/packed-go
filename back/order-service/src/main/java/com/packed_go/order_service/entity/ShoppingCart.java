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
@Table(name = "shopping_carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder.Default
    @Column(length = 20)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, COMPLETED

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<CartItem> items = new ArrayList<>();

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30); // 30 minutos para dar tiempo suficiente
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ============================================
    // Business Methods
    // ============================================

    /**
     * Verifica si el carrito ha expirado
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica si el carrito está activo
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * Calcula el monto total del carrito sumando todos los items
     */
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Agrega un item al carrito
     */
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    /**
     * Elimina un item del carrito
     */
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    /**
     * Marca el carrito como expirado
     */
    public void markAsExpired() {
        this.status = "EXPIRED";
    }

    /**
     * Marca el carrito como completado (checkout exitoso)
     */
    public void markAsCheckedOut() {
        this.status = "COMPLETED";
    }

    /**
     * Obtiene la cantidad total de items en el carrito
     */
    public int getTotalItems() {
        return items.size();
    }
}
