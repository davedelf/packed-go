package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {

    // ========== QUERIES ANTIGUAS (SIN MULTI-TENANT) ==========
    List<Consumption> findByActiveIsTrue();
    Optional<Consumption> findByName(String name);
    
    // ========== QUERIES MULTI-TENANT (NUEVAS) ==========
    /**
     * Encuentra todas las consumiciones activas creadas por un usuario específico
     */
    List<Consumption> findByCreatedByAndActiveIsTrue(Long createdBy);
    
    /**
     * Encuentra una consumición por ID y createdBy (validación de ownership)
     */
    Optional<Consumption> findByIdAndCreatedBy(Long id, Long createdBy);
    
    /**
     * Encuentra todas las consumiciones de un usuario (activas e inactivas)
     */
    List<Consumption> findByCreatedBy(Long createdBy);
}
