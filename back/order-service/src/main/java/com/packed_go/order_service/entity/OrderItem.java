package com.packed_go.order_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemConsumption> consumptions = new ArrayList<>();

    // ============================================
    // Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ============================================
    // Business Methods
    // ============================================

    /**
     * Calcula el subtotal del item (precio base + consumiciones)
     */
    public void calculateSubtotal() {
        BigDecimal consumptionsTotal = consumptions.stream()
                .map(OrderItemConsumption::getSubtotal)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                .add(consumptionsTotal);
    }

    /**
     * Agrega una consumición al item
     */
    public void addConsumption(OrderItemConsumption consumption) {
        consumptions.add(consumption);
        consumption.setOrderItem(this);
    }

    /**
     * Crea un OrderItem desde un CartItem
     */
    public static OrderItem fromCartItem(CartItem cartItem) {
        OrderItem orderItem = OrderItem.builder()
                .eventId(cartItem.getEventId())
                .eventName(cartItem.getEventName())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .subtotal(cartItem.getSubtotal())
                .build();

        // Copiar consumiciones
        cartItem.getConsumptions().forEach(cartConsumption -> {
            OrderItemConsumption orderConsumption = OrderItemConsumption.fromCartItemConsumption(cartConsumption);
            orderItem.addConsumption(orderConsumption);
        });

        return orderItem;
    }
}
