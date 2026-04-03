package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.pass.CreatePassDTO;
import com.packed_go.event_service.dtos.pass.PassDTO;
import com.packed_go.event_service.entities.Event;
import com.packed_go.event_service.entities.Pass;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.repositories.PassRepository;
import com.packed_go.event_service.services.PassService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassServiceImpl implements PassService {

    private final PassRepository passRepository;
    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public PassDTO createPass(CreatePassDTO createPassDTO) {
        return createPassForEvent(createPassDTO.getEventId(), createPassDTO.getCode());
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public PassDTO createPassForEvent(Long eventId, String code) {
        // Buscar el evento con bloqueo pesimista
        Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new RuntimeException("Event with id " + eventId + " not found"));

        // Verificar que el evento esté activo
        if (!event.isActive()) {
            throw new RuntimeException("Event is not active");
        }

        // Generar código único si no se proporciona
        String passCode = (code != null && !code.trim().isEmpty()) ? code : generateUniqueCode();

        // Verificar que el código sea único
        if (passRepository.findByCode(passCode).isPresent()) {
            throw new RuntimeException("Pass code already exists: " + passCode);
        }

        // Crear el pass
        Pass pass = new Pass(passCode, event);
        Pass savedPass = passRepository.save(pass);

        // Actualizar el evento
        event.addPass(savedPass);
        eventRepository.save(event);

        return modelMapper.map(savedPass, PassDTO.class);
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public PassDTO sellPass(Long passId, Long userId) {
        // Buscar el pass con bloqueo pesimista
        Pass pass = passRepository.findByIdWithLock(passId)
                .orElseThrow(() -> new RuntimeException("Pass with id " + passId + " not found"));

        // Verificar que el pass esté disponible
        if (!pass.isAvailable()) {
            throw new RuntimeException("Pass is not available for sale");
        }

        if (pass.isSold()) {
            throw new RuntimeException("Pass is already sold");
        }

        // Marcar como vendido
        pass.setSold(true);
        pass.setAvailable(false);
        pass.setSoldToUserId(userId);
        pass.setSoldAt(LocalDateTime.now());

        Pass savedPass = passRepository.save(pass);

        // Actualizar el evento
        Event event = pass.getEvent();
        event.markPassAsSold();
        eventRepository.save(event);

        return modelMapper.map(savedPass, PassDTO.class);
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public PassDTO sellPassByCode(String passCode, Long userId) {
        // Buscar el pass por código con bloqueo pesimista
        Pass pass = passRepository.findByCodeWithLock(passCode)
                .orElseThrow(() -> new RuntimeException("Pass with code " + passCode + " not found"));

        return sellPass(pass.getId(), userId);
    }

    @Override
    public PassDTO findById(Long id) {
        Pass pass = passRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pass with id " + id + " not found"));
        return modelMapper.map(pass, PassDTO.class);
    }

    @Override
    public PassDTO findByCode(String code) {
        Pass pass = passRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Pass with code " + code + " not found"));
        return modelMapper.map(pass, PassDTO.class);
    }

    @Override
    public List<PassDTO> findByEventId(Long eventId) {
        List<Pass> passes = passRepository.findByEvent_Id(eventId);
        return passes.stream()
                .map(pass -> modelMapper.map(pass, PassDTO.class))
                .toList();
    }

    @Override
    public List<PassDTO> findAvailablePassesByEventId(Long eventId) {
        List<Pass> passes = passRepository.findByEvent_IdAndAvailableTrue(eventId);
        return passes.stream()
                .map(pass -> modelMapper.map(pass, PassDTO.class))
                .toList();
    }

    @Override
    public List<PassDTO> findSoldPassesByEventId(Long eventId) {
        List<Pass> passes = passRepository.findByEvent_IdAndSoldTrue(eventId);
        return passes.stream()
                .map(pass -> modelMapper.map(pass, PassDTO.class))
                .toList();
    }

    @Override
    public List<PassDTO> findByUserId(Long userId) {
        List<Pass> passes = passRepository.findBySoldToUserId(userId);
        return passes.stream()
                .map(pass -> modelMapper.map(pass, PassDTO.class))
                .toList();
    }

    @Override
    public Long countAvailablePassesByEventId(Long eventId) {
        return passRepository.countAvailablePassesByEventId(eventId);
    }

    @Override
    public Long countSoldPassesByEventId(Long eventId) {
        return passRepository.countSoldPassesByEventId(eventId);
    }

    @Override
    @Transactional
    public boolean hasAvailablePasses(Long eventId) {
        Long count = passRepository.countAvailablePassesByEventId(eventId);
        return count > 0;
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = "PASS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (passRepository.findByCode(code).isPresent());
        return code;
    }

    @Recover
    public PassDTO recoverCreatePass(Exception ex, Long eventId, String code) {
        throw new RuntimeException("No se pudo crear el pass después de varios intentos debido a concurrencia: " + ex.getMessage(), ex);
    }

    @Recover
    public PassDTO recoverSellPass(Exception ex, Long passId, Long userId) {
        throw new RuntimeException("No se pudo vender el pass después de varios intentos debido a concurrencia: " + ex.getMessage(), ex);
    }
}
