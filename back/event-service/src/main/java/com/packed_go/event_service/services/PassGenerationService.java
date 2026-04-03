
package com.packed_go.event_service.services;

import com.packed_go.event_service.entities.Event;
import com.packed_go.event_service.entities.Pass;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.repositories.PassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassGenerationService {

    private final PassRepository passRepository;
    private final EventRepository eventRepository;

    @Transactional
    public List<Pass> generatePassesForEvent(Long eventId, Integer quantity) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        List<Pass> newPasses = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            String code = generateUniqueCode(eventId);
            Pass pass = new Pass();
            pass.setCode(code);
            pass.setEvent(event);
            pass.setAvailable(true);
            pass.setSold(false);
            newPasses.add(pass);
        }

        List<Pass> savedPasses = passRepository.saveAll(newPasses);

        event.setTotalPasses(event.getTotalPasses() + quantity);
        event.setAvailablePasses(event.getAvailablePasses() + quantity);
        eventRepository.save(event);

        log.info("✅ Generated {} passes for event {}", quantity, eventId);
        return savedPasses;
    }

    private String generateUniqueCode(Long eventId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("PKG-%d-%s-%s", eventId, timestamp, random);
    }
}
