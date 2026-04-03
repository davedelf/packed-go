package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.eventCategory.CreateEventCategoryDTO;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface EventCategoryService {

    EventCategoryDTO findById(Long id);

    List<EventCategoryDTO> findByActiveIsTrue();

    List<EventCategoryDTO> findAll();

    EventCategoryDTO create(CreateEventCategoryDTO createEventCategoryDto);

    EventCategoryDTO update(Long id, CreateEventCategoryDTO createEventCategoryDto);

    void delete(Long id);

    @Transactional
    EventCategoryDTO deleteLogical(Long id);

    @Transactional
    EventCategoryDTO updateStatus(Long id);
}
