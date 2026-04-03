package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDTO;
import com.packed_go.event_service.entities.Consumption;
import jakarta.transaction.Transactional;

import java.util.List;

public interface ConsumptionService {
    ConsumptionDTO findById(Long id);

    List<ConsumptionDTO> findAll();

    List<ConsumptionDTO> findAllByIsActive();

    ConsumptionDTO createConsumption(CreateConsumptionDTO createEventDto);

    ConsumptionDTO updateConsumption(Long id, CreateConsumptionDTO eventDto);

    void delete(Long id);

    @Transactional
    ConsumptionDTO deleteLogical(Long id);
    
    ConsumptionDTO mapToDTO(Consumption consumption);

}
