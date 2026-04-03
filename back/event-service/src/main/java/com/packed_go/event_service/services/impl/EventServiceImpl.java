package com.packed_go.event_service.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.event.CreateEventDTO;
import com.packed_go.event_service.dtos.event.EventDTO;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDTO;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.entities.Event;
import com.packed_go.event_service.entities.EventCategory;
import com.packed_go.event_service.entities.Pass;
import com.packed_go.event_service.entities.Ticket;
import com.packed_go.event_service.exceptions.ResourceNotFoundException;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.repositories.EventCategoryRepository;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.repositories.PassRepository;
import com.packed_go.event_service.repositories.TicketRepository;
import com.packed_go.event_service.services.ConsumptionService;
import com.packed_go.event_service.services.EventService;
import com.packed_go.event_service.services.PassGenerationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    @Autowired
    private final EventRepository eventRepository;

    @Autowired
    private final ModelMapper modelMapper;

    @Autowired
    private final EventCategoryRepository eventCategoryRepository;
    
    @Autowired
    private final ConsumptionRepository consumptionRepository;
    
    @Autowired
    private final ConsumptionService consumptionService;
    
    @Autowired
    private final PassRepository passRepository;
    
    @Autowired
    private final TicketRepository ticketRepository;
    
    private final PassGenerationService passGenerationService;

    private EventDTO mapEventToDTO(Event event) {
        EventDTO eventDTO = modelMapper.map(event, EventDTO.class);
        
        // Indicar si tiene imagen almacenada
        eventDTO.setHasImageData(event.getImageData() != null && event.getImageData().length > 0);
        
        // Mapear explícitamente locationName
        eventDTO.setLocationName(event.getLocationName());
        
        // Mapear la categoría completa
        if (event.getCategory() != null) {
            EventCategoryDTO categoryDTO = modelMapper.map(event.getCategory(), EventCategoryDTO.class);
            eventDTO.setCategory(categoryDTO);
            eventDTO.setCategoryId(event.getCategory().getId());
        }
        
        // Cargar las consumptions asociadas al evento desde la tabla intermedia
        if (event.getConsumptions() != null && !event.getConsumptions().isEmpty()) {
            List<ConsumptionDTO> consumptionDTOs = event.getConsumptions().stream()
                .filter(c -> c.isActive())
                .map(c -> consumptionService.mapToDTO(c))
                .toList();
            eventDTO.setAvailableConsumptions(consumptionDTOs);
        }
        
        return eventDTO;
    }

    @Override
    public EventDTO findById(Long id) {
        Optional<Event> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            return mapEventToDTO(eventExist.get());
        } else {
            throw new RuntimeException("Event with id" + id + " not found");
        }

    }

    @Override
    public List<EventDTO> findAll() {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream()
                .map(this::mapEventToDTO)
                .toList();
    }

    @Override
    public List<EventDTO> findByCreatedBy(Long createdBy) {
        List<Event> eventEntities = eventRepository.findByCreatedBy(createdBy);
        return eventEntities.stream()
                .map(this::mapEventToDTO)
                .toList();
    }

    @Override
    public List<EventDTO> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Event> events = eventRepository.findAllById(ids);
        return events.stream()
                .map(this::mapEventToDTO)
                .toList();
    }


    @Override
    public List<EventDTO> findAllByStatus(String status) {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getStatus().equalsIgnoreCase(status)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }

    @Override
    public List<EventDTO> findAllByEventDate(LocalDateTime eventDate) {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getEventDate().equals(eventDate)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }

    @Override
    public List<EventDTO> findAllByEventDateAndStatus(LocalDateTime eventDate, String status) {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getStatus().equalsIgnoreCase(status) && entity.getEventDate().equals(eventDate)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }

    /*
    @Override
    public List<EventDTO> findByLocation(Point location) {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getLocation().equals(location)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }
    */

    @Override
    public EventDTO createEvent(CreateEventDTO createEventDto) {
        EventCategory category = eventCategoryRepository.findById(createEventDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + createEventDto.getCategoryId() + " not found"));

        // Validar que endTime > startTime
        if (createEventDto.getStartTime() != null && createEventDto.getEndTime() != null) {
            if (!createEventDto.getEndTime().isAfter(createEventDto.getStartTime())) {
                throw new IllegalArgumentException("La fecha y hora de finalización debe ser mayor que la de inicio");
            }
        }

        Event event = new Event(); // Constructor inicializa status="ACTIVE", active=true, timestamps
        
        System.out.println("AFTER Constructor - Event status: " + event.getStatus());
        
        // Mapeo MANUAL para evitar problemas con ModelMapper
        event.setName(createEventDto.getName());
        event.setDescription(createEventDto.getDescription());
        event.setEventDate(createEventDto.getEventDate());
        event.setStartTime(createEventDto.getStartTime());
        event.setEndTime(createEventDto.getEndTime());
        event.setLat(createEventDto.getLat());
        event.setLng(createEventDto.getLng());
        event.setLocationName(createEventDto.getLocationName());
        event.setMaxCapacity(createEventDto.getMaxCapacity());
        event.setCurrentCapacity(createEventDto.getCurrentCapacity() != null ? createEventDto.getCurrentCapacity() : 0);
        event.setBasePrice(createEventDto.getBasePrice());
        event.setImageUrl(createEventDto.getImageUrl());
        event.setCreatedBy(createEventDto.getCreatedBy());
        event.setCategory(category);
        
        System.out.println("BEFORE save - Event status: " + event.getStatus());
        System.out.println("BEFORE save - Event name: " + event.getName());
        
        // El constructor ya inicializa: status, active, timestamps, passes, etc.
        // No es necesario setearlos de nuevo a menos que el DTO los override

        // Asociar consumiciones si vienen en el DTO
        if (createEventDto.getConsumptionIds() != null && !createEventDto.getConsumptionIds().isEmpty()) {
            List<Consumption> consumptions = consumptionRepository.findAllById(createEventDto.getConsumptionIds());
            
            // Validar que todas las consumiciones pertenecen al mismo admin que crea el evento
            for (Consumption consumption : consumptions) {
                if (!consumption.getCreatedBy().equals(createEventDto.getCreatedBy())) {
                    throw new IllegalArgumentException(
                        "No se puede asociar la consumición '" + consumption.getName() + 
                        "' porque pertenece a otro administrador"
                    );
                }
            }
            
            event.getConsumptions().clear();
            event.getConsumptions().addAll(consumptions);
        }

        Event savedEvent = eventRepository.save(event);

        // Generate passes for the event
        if (savedEvent.getMaxCapacity() != null && savedEvent.getMaxCapacity() > 0) {
            passGenerationService.generatePassesForEvent(savedEvent.getId(), savedEvent.getMaxCapacity());
        }

        return modelMapper.map(savedEvent, EventDTO.class);
    }


    @Override
    public EventDTO updateEvent(Long id, EventDTO eventDto) {
        Event entity = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event con id " + id + " no encontrado"));

        // Validar que endTime > startTime
        if (eventDto.getStartTime() != null && eventDto.getEndTime() != null) {
            if (!eventDto.getEndTime().isAfter(eventDto.getStartTime())) {
                throw new IllegalArgumentException("La fecha y hora de finalización debe ser mayor que la de inicio");
            }
        }

        // Si viene una categoryId en el DTO, la buscamos y la asignamos
        if (eventDto.getCategoryId() != null) {
            EventCategory category = eventCategoryRepository.findById(eventDto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category with id " + eventDto.getCategoryId() + " not found"));
            entity.setCategory(category);
        }

        // Actualizamos los campos relevantes desde el DTO
        if (eventDto.getName() != null) entity.setName(eventDto.getName());
        if (eventDto.getDescription() != null) entity.setDescription(eventDto.getDescription());
        if (eventDto.getEventDate() != null) entity.setEventDate(eventDto.getEventDate());
        if (eventDto.getStartTime() != null) entity.setStartTime(eventDto.getStartTime());
        if (eventDto.getEndTime() != null) entity.setEndTime(eventDto.getEndTime());
        if (eventDto.getLat() != 0) entity.setLat(eventDto.getLat());
        if (eventDto.getLng() != 0) entity.setLng(eventDto.getLng());
        if (eventDto.getLocationName() != null) entity.setLocationName(eventDto.getLocationName());
        if (eventDto.getMaxCapacity() != null) entity.setMaxCapacity(eventDto.getMaxCapacity());
        if (eventDto.getCurrentCapacity() != null) entity.setCurrentCapacity(eventDto.getCurrentCapacity());
        if (eventDto.getBasePrice() != null) entity.setBasePrice(eventDto.getBasePrice());
        if (eventDto.getImageUrl() != null) entity.setImageUrl(eventDto.getImageUrl());
        
        // Solo actualizar status si viene en el DTO (no null)
        if (eventDto.getStatus() != null) {
            entity.setStatus(eventDto.getStatus());
        }
        
        entity.setActive(eventDto.isActive());
        // No tocar createdAt/createdBy para preservar histórico; actualizamos updatedAt
        entity.setUpdatedAt(LocalDateTime.now());

        // Campos de Pass (si vienen en el DTO)
        if (eventDto.getTotalPasses() != null) entity.setTotalPasses(eventDto.getTotalPasses());
        if (eventDto.getAvailablePasses() != null) entity.setAvailablePasses(eventDto.getAvailablePasses());
        if (eventDto.getSoldPasses() != null) entity.setSoldPasses(eventDto.getSoldPasses());

        // Actualizar consumiciones asociadas si vienen en el DTO
        // SEGURIDAD MULTI-TENANT: Solo permitir consumiciones del mismo admin que creó el evento
        if (eventDto.getConsumptionIds() != null) {
            List<Consumption> consumptions = consumptionRepository.findAllById(eventDto.getConsumptionIds());
            
            // Validar que todas las consumiciones pertenecen al mismo admin que creó el evento
            for (Consumption consumption : consumptions) {
                if (!consumption.getCreatedBy().equals(entity.getCreatedBy())) {
                    throw new IllegalArgumentException(
                        "No se puede asociar la consumición '" + consumption.getName() + 
                        "' porque pertenece a otro administrador"
                    );
                }
            }
            
            entity.getConsumptions().clear();
            entity.getConsumptions().addAll(consumptions);
        }

        Event updatedEntity = eventRepository.save(entity);
        return mapEventToDTO(updatedEntity);
    }

    @Override
    public void delete(Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
        } else {
            throw new RuntimeException("Event con id" + id + " no encontrado");
        }
    }

    @Transactional
    @Override
    public EventDTO deleteLogical(Long id) {
        Optional<Event> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            Event entity = eventExist.get();
            entity.setActive(false);
            
            // Desactivar todos los pases asociados al evento
            List<Pass> passes = passRepository.findByEvent_Id(id);
            for (Pass pass : passes) {
                pass.setActive(false);
                pass.setAvailable(false);
            }
            passRepository.saveAll(passes);
            
            // Desactivar todos los tickets asociados al evento
            List<Ticket> tickets = ticketRepository.findByEventId(id);
            for (Ticket ticket : tickets) {
                ticket.setActive(false);
            }
            ticketRepository.saveAll(tickets);
            
            Event updatedentity = eventRepository.save(entity);
            return modelMapper.map(updatedentity, EventDTO.class);
        } else {
            throw new RuntimeException("Event con id" + id + " no encontrado");
        }
    }

    @Override
    public List<ConsumptionDTO> getEventConsumptions(Long eventId) {
        // Verificar que el evento existe
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event with id " + eventId + " not found"));
        
        // Obtener las consumiciones activas del creador del evento
        List<Consumption> consumptions = consumptionRepository.findByCreatedByAndActiveIsTrue(event.getCreatedBy());
        
        // Mapear a DTOs
        return consumptions.stream()
                .map(consumption -> modelMapper.map(consumption, ConsumptionDTO.class))
                .toList();
    }

    @Override
    @Transactional
    public void saveEventImage(Long eventId, byte[] imageData, String contentType) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event with id " + eventId + " not found"));
        
        event.setImageData(imageData);
        event.setImageContentType(contentType);
        eventRepository.save(event);
    }

    @Override
    public byte[] getEventImage(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event with id " + eventId + " not found"));
        
        return event.getImageData();
    }
}
