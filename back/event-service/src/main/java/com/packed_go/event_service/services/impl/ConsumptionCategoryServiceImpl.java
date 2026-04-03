package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.consumptionCategory.ConsumptionCategoryDTO;
import com.packed_go.event_service.dtos.consumptionCategory.CreateConsumptionCategoryDTO;
import com.packed_go.event_service.entities.ConsumptionCategory;
import com.packed_go.event_service.repositories.ConsumptionCategoryRepository;
import com.packed_go.event_service.services.ConsumptionCategoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ConsumptionCategoryServiceImpl implements ConsumptionCategoryService {


    @Autowired
    private final ConsumptionCategoryRepository consumptionCategoryRepository;
    @Autowired
    private final ModelMapper modelMapper;

    @Override
    public ConsumptionCategoryDTO findById(Long id) {
        Optional<ConsumptionCategory> consumptionExist = consumptionCategoryRepository.findById(id);
        if (consumptionExist.isPresent()) {
            return modelMapper.map(consumptionExist.get(), ConsumptionCategoryDTO.class);
        } else {
            throw new RuntimeException("ConsumptionCategory with id " + id + " not found");
        }
    }

    @Override
    public List<ConsumptionCategoryDTO> findByActiveIsTrue() {
        List<ConsumptionCategory> categoryEntities = consumptionCategoryRepository.findByActiveIsTrue();
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, ConsumptionCategoryDTO.class))
                .toList();
    }
    
    @Override
    public List<ConsumptionCategoryDTO> findByActiveIsTrueAndCreatedBy(Long createdBy) {
        List<ConsumptionCategory> categoryEntities = consumptionCategoryRepository.findByActiveIsTrueAndCreatedBy(createdBy);
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, ConsumptionCategoryDTO.class))
                .toList();
    }

    @Override
    public List<ConsumptionCategoryDTO> findAll() {
        List<ConsumptionCategory> categoryEntities = consumptionCategoryRepository.findAll();
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, ConsumptionCategoryDTO.class))
                .toList();
    }

    @Override
    public ConsumptionCategoryDTO create(CreateConsumptionCategoryDTO createConsumptionCategoryDto) {
        // 🔒 Multitenant: Validar que no exista una categoría con el mismo nombre PARA ESTE ADMIN
        Long createdBy = createConsumptionCategoryDto.getCreatedBy();
        if (createdBy != null) {
            Optional<ConsumptionCategory> categoryExist = consumptionCategoryRepository
                    .findByNameAndCreatedBy(createConsumptionCategoryDto.getName(), createdBy);
            if (categoryExist.isPresent()) {
                throw new RuntimeException("Ya tienes una categoría de consumo con este nombre");
            }
        } else {
            // Fallback: Si no hay createdBy, validar globalmente (legacy)
            Optional<ConsumptionCategory> categoryExist = consumptionCategoryRepository
                    .findByName(createConsumptionCategoryDto.getName());
            if (categoryExist.isPresent()) {
                throw new RuntimeException("Consumption category ya existe");
            }
        }
        
        ConsumptionCategory entity = modelMapper.map(createConsumptionCategoryDto, ConsumptionCategory.class);
        entity.setActive(true);
        return modelMapper.map(consumptionCategoryRepository.save(entity), ConsumptionCategoryDTO.class);
    }

    @Override
    public ConsumptionCategoryDTO update(Long id, CreateConsumptionCategoryDTO createConsumptionCategoryDto) {
        Optional<ConsumptionCategory> categoryExist = consumptionCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            ConsumptionCategory entity = categoryExist.get();
            entity.setName(createConsumptionCategoryDto.getName());
            return modelMapper.map(consumptionCategoryRepository.save(entity), ConsumptionCategoryDTO.class);
        } else {
            throw new RuntimeException("Consumption category con id " + id + " no encontrado");
        }
    }

    @Override
    public void delete(Long id) {
        if (consumptionCategoryRepository.existsById(id)) {
            consumptionCategoryRepository.deleteById(id);
        } else {
            throw new RuntimeException("Consumption category category con id " + id + " no encontrado");
        }
    }

    @Override
    public ConsumptionCategoryDTO deleteLogical(Long id) {
        Optional<ConsumptionCategory> categoryExist = consumptionCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            ConsumptionCategory entity = categoryExist.get();
            entity.setActive(false);
            return modelMapper.map(consumptionCategoryRepository.save(entity), ConsumptionCategoryDTO.class);

        } else {
            throw new RuntimeException("Event category con id " + id + " no encontrado");
        }
    }

    @Override
    public ConsumptionCategoryDTO updateStatus(Long id) {
        ConsumptionCategory entity = consumptionCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consumption category not found with id: " + id));
        entity.setActive(!entity.isActive());
        ConsumptionCategory updatedEntity = consumptionCategoryRepository.save(entity);
        return modelMapper.map(updatedEntity, ConsumptionCategoryDTO.class);
    }
}
