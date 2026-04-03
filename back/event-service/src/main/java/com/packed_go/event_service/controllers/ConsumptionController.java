package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDTO;
import com.packed_go.event_service.security.JwtTokenValidator;
import com.packed_go.event_service.services.impl.ConsumptionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/event-service/consumption")
@RequiredArgsConstructor
public class ConsumptionController {
    private static final Logger log = LoggerFactory.getLogger(ConsumptionController.class);
    private final ConsumptionServiceImpl service;
    private final JwtTokenValidator jwtValidator;

    /**
     * 🔐 Helper: Extrae userId del JWT
     */
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }
        
        return jwtValidator.getUserIdFromToken(token);
    }

    /**
     * � GET /consumption/{id} - Obtener consumición por ID (valida ownership)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsumptionDTO> getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} fetching consumption {}", userId, id);
        
        ConsumptionDTO consumption = service.findById(id);
        
        // Validar ownership
        if (!consumption.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} attempted to access consumption {} owned by user {}", 
                    userId, id, consumption.getCreatedBy());
            throw new RuntimeException("Access denied: You can only access your own consumptions");
        }
        
        return ResponseEntity.ok(consumption);
    }

    /**
     * 🔒 GET /consumption - Obtener todas las consumiciones del usuario autenticado
     */
    @GetMapping
    public ResponseEntity<List<ConsumptionDTO>> getAll(
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} fetching all their consumptions", userId);
        
        return ResponseEntity.ok(service.findByCreatedBy(userId));
    }

    /**
     * 🔒 POST /consumption - Crear consumición (inyecta createdBy desde JWT)
     */
    @PostMapping
    public ResponseEntity<ConsumptionDTO> create(
            @RequestBody CreateConsumptionDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} creating consumption: {}", userId, dto.getName());
        
        ConsumptionDTO created = service.createConsumption(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 🔒 PUT /consumption/{id} - Actualizar consumición (valida ownership)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConsumptionDTO> update(
            @PathVariable Long id, 
            @RequestBody CreateConsumptionDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} updating consumption {}", userId, id);
        
        return ResponseEntity.ok(service.updateConsumption(id, dto, userId));
    }

    /**
     * 🔒 DELETE /consumption/{id} - Eliminar consumición físicamente (valida ownership)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} deleting consumption {}", userId, id);
        
        service.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 🔒 DELETE /consumption/logical/{id} - Desactivar consumición (valida ownership)
     */
    @DeleteMapping("/logical/{id}")
    public ResponseEntity<ConsumptionDTO> deleteLogical(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} deactivating consumption {}", userId, id);
        
        return ResponseEntity.ok(service.deleteLogical(id, userId));
    }
}
