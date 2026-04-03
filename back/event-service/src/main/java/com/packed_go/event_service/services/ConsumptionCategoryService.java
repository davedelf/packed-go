package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.consumptionCategory.ConsumptionCategoryDTO;
import com.packed_go.event_service.dtos.consumptionCategory.CreateConsumptionCategoryDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface ConsumptionCategoryService {
    ConsumptionCategoryDTO findById(Long id);

    List<ConsumptionCategoryDTO> findByActiveIsTrue();
    
    // 🔒 Multitenant: Categorías activas del admin
    List<ConsumptionCategoryDTO> findByActiveIsTrueAndCreatedBy(Long createdBy);

    List<ConsumptionCategoryDTO> findAll();

    ConsumptionCategoryDTO create(CreateConsumptionCategoryDTO createConsumptionCategoryDto);

    ConsumptionCategoryDTO update(Long id, CreateConsumptionCategoryDTO createConsumptionCategoryDto);

    void delete(Long id);

    @Transactional
    ConsumptionCategoryDTO deleteLogical(Long id);

    @Transactional
    ConsumptionCategoryDTO updateStatus(Long id);
}

