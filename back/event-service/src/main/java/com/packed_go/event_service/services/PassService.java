package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.pass.CreatePassDTO;
import com.packed_go.event_service.dtos.pass.PassDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface PassService {

    @Transactional
    PassDTO createPass(CreatePassDTO createPassDTO);

    @Transactional
    PassDTO createPassForEvent(Long eventId, String code);

    @Transactional
    PassDTO sellPass(Long passId, Long userId);

    @Transactional
    PassDTO sellPassByCode(String passCode, Long userId);

    PassDTO findById(Long id);

    PassDTO findByCode(String code);

    List<PassDTO> findByEventId(Long eventId);

    List<PassDTO> findAvailablePassesByEventId(Long eventId);

    List<PassDTO> findSoldPassesByEventId(Long eventId);

    List<PassDTO> findByUserId(Long userId);

    Long countAvailablePassesByEventId(Long eventId);

    Long countSoldPassesByEventId(Long eventId);

    @Transactional
    boolean hasAvailablePasses(Long eventId);
}
