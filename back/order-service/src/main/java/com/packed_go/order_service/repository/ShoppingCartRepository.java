package com.packed_go.order_service.repository;

import com.packed_go.order_service.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    /**
     * Busca el carrito activo de un usuario
     */
    Optional<ShoppingCart> findByUserIdAndStatus(Long userId, String status);
    
    /**
     * Busca el carrito activo de un usuario con items cargados (EAGER)
     * Retorna el más reciente si hay múltiples (por updatedAt DESC)
     * Nota: No se pueden hacer fetch de múltiples colecciones con ORDER BY en Hibernate
     * El ordenamiento se debe hacer en la capa de servicio si es necesario
     */
    @Query("SELECT DISTINCT c FROM ShoppingCart c " +
           "LEFT JOIN FETCH c.items " +
           "WHERE c.userId = :userId AND c.status = :status " +
           "ORDER BY c.updatedAt DESC")
    List<ShoppingCart> findByUserIdAndStatusWithItems(@Param("userId") Long userId, @Param("status") String status);

    /**
     * Busca todos los carritos de un usuario
     */
    List<ShoppingCart> findByUserId(Long userId);

    /**
     * Busca carritos expirados que aún están con status ACTIVE
     */
    List<ShoppingCart> findByStatusAndExpiresAtBefore(String status, LocalDateTime dateTime);

    /**
     * Busca carritos activos que están por expirar (para notificaciones)
     */
    @Query("SELECT c FROM ShoppingCart c WHERE c.status = 'ACTIVE' " +
           "AND c.expiresAt BETWEEN :now AND :futureTime")
    List<ShoppingCart> findCartsExpiringBetween(
        @Param("now") LocalDateTime now,
        @Param("futureTime") LocalDateTime futureTime
    );

    /**
     * Verifica si existe un carrito activo para un usuario
     */
    boolean existsByUserIdAndStatus(Long userId, String status);

    /**
     * Elimina carritos expirados más antiguos que una fecha
     */
    void deleteByStatusAndCreatedAtBefore(String status, LocalDateTime dateTime);
}
