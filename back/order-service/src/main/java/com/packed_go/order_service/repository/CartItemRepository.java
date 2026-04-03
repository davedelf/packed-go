package com.packed_go.order_service.repository;

import com.packed_go.order_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Busca todos los items de un carrito
     */
    List<CartItem> findByCartId(Long cartId);

    /**
     * Busca un item específico por carrito y evento
     */
    Optional<CartItem> findByCartIdAndEventId(Long cartId, Long eventId);

    /**
     * Busca items por evento (útil para verificar cuántos usuarios tienen un evento en el carrito)
     */
    List<CartItem> findByEventId(Long eventId);

    /**
     * Cuenta cuántos items tiene un carrito
     */
    long countByCartId(Long cartId);

    /**
     * Verifica si existe un item para un evento en un carrito específico
     */
    boolean existsByCartIdAndEventId(Long cartId, Long eventId);

    /**
     * Elimina todos los items de un carrito
     */
    void deleteByCartId(Long cartId);
}
