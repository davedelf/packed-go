package com.packed_go.event_service.services.impl;

import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.packed_go.event_service.dtos.eventCategory.CreateEventCategoryDTO;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDTO;
import com.packed_go.event_service.entities.EventCategory;
import com.packed_go.event_service.repositories.EventCategoryRepository;
import com.packed_go.event_service.services.EventCategoryService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventCategoryServiceImpl implements EventCategoryService {

    @Autowired
    private final EventCategoryRepository eventCategoryRepository;
    @Autowired
    private final ModelMapper modelMapper;

    @Override
    public EventCategoryDTO findById(Long id) {
        Optional<EventCategory> categoryExist = eventCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            return modelMapper.map(categoryExist, EventCategoryDTO.class);
        } else {
            throw new RuntimeException("Event con id " + id + " no encontrado");
        }
    }

    @Override
    public List<EventCategoryDTO> findByActiveIsTrue() {
        List<EventCategory> categoryEntities = eventCategoryRepository.findByActiveIsTrue();
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, EventCategoryDTO.class))
                .toList();
    }

    @Override
    public List<EventCategoryDTO> findAll() {
        List<EventCategory> categoryEntities = eventCategoryRepository.findAll();
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, EventCategoryDTO.class))
                .toList();
    }

    @Override
    public EventCategoryDTO create(CreateEventCategoryDTO createEventCategoryDto) {
        // 🔒 Multitenant: Validar que no exista una categoría con el mismo nombre PARA ESTE ADMIN
        Long createdBy = createEventCategoryDto.getCreatedBy();
        if (createdBy != null) {
            Optional<EventCategory> categoryExist = eventCategoryRepository
                    .findByNameAndCreatedBy(createEventCategoryDto.getName(), createdBy);
            if (categoryExist.isPresent()) {
                throw new RuntimeException("Ya tienes una categoría de evento con este nombre");
            }
        } else {
            // Fallback: Si no hay createdBy, validar globalmente (legacy)
            Optional<EventCategory> categoryExist = eventCategoryRepository
                    .findByName(createEventCategoryDto.getName());
            if (categoryExist.isPresent()) {
                throw new RuntimeException("Event category ya existe");
            }
        }
        
        EventCategory entity = modelMapper.map(createEventCategoryDto, EventCategory.class);
        entity.setActive(true);
        return modelMapper.map(eventCategoryRepository.save(entity), EventCategoryDTO.class);
    }

    @Override
    public EventCategoryDTO update(Long id, CreateEventCategoryDTO createEventCategoryDto) {
        Optional<EventCategory> categoryExist = eventCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            EventCategory existingEntity = categoryExist.get();
            
            // Actualizar los campos que vienen en el DTO
            existingEntity.setName(createEventCategoryDto.getName());
            if (createEventCategoryDto.getDescription() != null) {
                existingEntity.setDescription(createEventCategoryDto.getDescription());
            }
            
            // Si viene el campo active en el DTO, usarlo; sino preservar el existente
            if (createEventCategoryDto.getActive() != null) {
                existingEntity.setActive(createEventCategoryDto.getActive());
            }
            
            // Preservar createdBy del registro existente
            
            EventCategory saved = eventCategoryRepository.save(existingEntity);
            return modelMapper.map(saved, EventCategoryDTO.class);
        } else {
            throw new RuntimeException("Event category con id " + id + " no encontrado");
        }
    }

    @Override
    public void delete(Long id) {
        if (eventCategoryRepository.existsById(id)) {
            eventCategoryRepository.deleteById(id);
        } else {
            throw new RuntimeException("Event category con id " + id + " no encontrado");
        }
    }

    @Override
    @Transactional
    public EventCategoryDTO deleteLogical(Long id) {
        Optional<EventCategory> categoryExist = eventCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            EventCategory entity = categoryExist.get();
            entity.setActive(false);
            return modelMapper.map(eventCategoryRepository.save(entity), EventCategoryDTO.class);

        } else {
            throw new RuntimeException("Event category con id " + id + " no encontrado");
        }

    }

    @Transactional
    @Override
    public EventCategoryDTO updateStatus(Long id) {
        EventCategory entity = eventCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        entity.setActive(!entity.isActive());
        EventCategory updatedEntity = eventCategoryRepository.save(entity);
        return modelMapper.map(updatedEntity, EventCategoryDTO.class);
    }

}
