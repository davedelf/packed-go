package com.packed_go.event_service.controllers;

import com.packed_go.event_service.services.PassGenerationService;
import com.packed_go.event_service.security.JwtTokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/event-service/passes")
@RequiredArgsConstructor
public class PassController {

    private final PassGenerationService passGenerationService;
    private final JwtTokenValidator jwtValidator;

    @PostMapping("/generate/{eventId}/{quantity}")
    public ResponseEntity<String> generatePassesForEvent(
            @PathVariable Long eventId,
            @PathVariable Integer quantity,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }
        // We could add an extra validation to check if the user is an ADMIN and owns the event

        passGenerationService.generatePassesForEvent(eventId, quantity);
        return ResponseEntity.ok(quantity + " passes generated successfully for event " + eventId);
    }
}