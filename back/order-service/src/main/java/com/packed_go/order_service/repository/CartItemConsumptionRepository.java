package com.packed_go.order_service.repository;

import com.packed_go.order_service.entity.CartItemConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemConsumptionRepository extends JpaRepository<CartItemConsumption, Long> {

    /**
     * Busca todas las consumiciones de un cart item
     */
    List<CartItemConsumption> findByCartItemId(Long cartItemId);

    /**
     * Busca consumiciones por ID de consumición (referencia externa)
     */
    List<CartItemConsumption> findByConsumptionId(Long consumptionId);

    /**
     * Elimina todas las consumiciones de un cart item
     */
    void deleteByCartItemId(Long cartItemId);
}
