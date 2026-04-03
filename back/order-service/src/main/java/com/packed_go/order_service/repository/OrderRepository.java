package com.packed_go.order_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.packed_go.order_service.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Busca una orden por su número de orden
     */
    Optional<Order> findByOrderNumber(String orderNumber);
    
    /**
     * Busca todas las órdenes de un usuario
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Busca órdenes por estado
     */
    List<Order> findByStatus(Order.OrderStatus status);
    
    /**
     * Busca órdenes por usuario y estado
     */
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Order.OrderStatus status);
    
    /**
     * Busca una orden por cartId
     */
    Optional<Order> findByCartId(Long cartId);
    
    /**
     * Busca una orden por preferenceId de MercadoPago
     */
    Optional<Order> findByPaymentPreferenceId(String paymentPreferenceId);
    
    /**
     * Busca todas las órdenes de un organizador (adminId)
     */
    List<Order> findByAdminIdOrderByCreatedAtDesc(Long adminId);
}
