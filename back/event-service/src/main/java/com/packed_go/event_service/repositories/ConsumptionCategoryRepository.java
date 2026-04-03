package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.ConsumptionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionCategoryRepository extends JpaRepository<ConsumptionCategory,Long> {
    Optional<ConsumptionCategory> findById(Long id);

    List<ConsumptionCategory> findByActiveIsTrue();

    Optional<ConsumptionCategory> findByName(String name);
    
    // 🔒 Multitenant: Buscar por nombre Y admin
    Optional<ConsumptionCategory> findByNameAndCreatedBy(String name, Long createdBy);
    
    // 🔒 Multitenant: Obtener categorías del admin
    List<ConsumptionCategory> findByCreatedBy(Long createdBy);
    
    List<ConsumptionCategory> findByActiveIsTrueAndCreatedBy(Long createdBy);
}
