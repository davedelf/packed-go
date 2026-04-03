package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.eventCategory.CreateEventCategoryDTO;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDTO;
import com.packed_go.event_service.security.JwtTokenValidator;
import com.packed_go.event_service.services.EventCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/event-service/category")
@RequiredArgsConstructor
public class EventCategoryController {
    private static final Logger log = LoggerFactory.getLogger(EventCategoryController.class);
    private final EventCategoryService eventCategoryService;
    private final ModelMapper modelMapper;
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
     * 🔒 POST /category - Crear categoría de evento (solo admins)
     */
    @PostMapping
    public ResponseEntity<EventCategoryDTO> create(
            @RequestBody CreateEventCategoryDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} creating event category: {}", userId, dto.getName());
        
        // Inyectar createdBy desde JWT
        if (dto.getCreatedBy() == null) {
            dto.setCreatedBy(userId);
        } else if (!dto.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to create category with different createdBy: {}", userId, dto.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        EventCategoryDTO created = eventCategoryService.create(dto);
        if (created != null) {
            return ResponseEntity.ok(created);
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventCategoryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventCategoryService.findById(id));
    }

    /**
     * 🔒 GET /category - Listar categorías (admins ven solo las suyas, customers ven todas)
     */
    @GetMapping
    public ResponseEntity<List<EventCategoryDTO>> getAll(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Si no hay token, devolver todas (para público)
        if (authHeader == null || authHeader.isEmpty()) {
            return ResponseEntity.ok(eventCategoryService.findAll());
        }
        
        try {
            Long userId = extractUserIdFromToken(authHeader);
            // TODO: Aquí deberíamos verificar si es ADMIN o CUSTOMER desde el JWT
            // Por ahora, asumimos que si viene con token, filtramos por userId
            log.info("🔒 User {} fetching event categories", userId);
            
            List<EventCategoryDTO> categories = eventCategoryService.findAll().stream()
                    .filter(cat -> cat.getCreatedBy() != null && cat.getCreatedBy().equals(userId))
                    .toList();
            
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            // Si falla la validación del token, devolver todas
            return ResponseEntity.ok(eventCategoryService.findAll());
        }
    }

    /**
     * 🔓 GET /category/active - Categorías activas (público para customers)
     */
    @GetMapping("/active")
    public ResponseEntity<List<EventCategoryDTO>> getAllActive() {
        return ResponseEntity.ok(eventCategoryService.findByActiveIsTrue());
    }

    /**
     * 🔒 PUT /category/{id} - Actualizar categoría (valida ownership)
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventCategoryDTO> update(
            @PathVariable Long id, 
            @RequestBody CreateEventCategoryDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} updating event category {}", userId, id);
        
        // Validar ownership
        EventCategoryDTO existing = eventCategoryService.findById(id);
        if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to update category {} owned by {}", userId, id, existing.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(eventCategoryService.update(id, dto));
    }

    /**
     * 🔒 DELETE /category/{id} - Eliminar categoría (valida ownership)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} deleting event category {}", userId, id);
        
        // Validar ownership
        EventCategoryDTO existing = eventCategoryService.findById(id);
        if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to delete category {} owned by {}", userId, id, existing.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        eventCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 🔒 DELETE /category/logical/{id} - Desactivar categoría (valida ownership)
     */
    @DeleteMapping("/logical/{id}")
    public ResponseEntity<EventCategoryDTO> deleteLogical(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} deactivating event category {}", userId, id);
        
        // Validar ownership
        EventCategoryDTO existing = eventCategoryService.findById(id);
        if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to deactivate category {} owned by {}", userId, id, existing.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(modelMapper.map(eventCategoryService.deleteLogical(id), EventCategoryDTO.class));
    }

    /**
     * 🔒 PUT /category/status/{id} - Cambiar estado de categoría (valida ownership)
     */
    @PutMapping("/status/{id}")
    public ResponseEntity<EventCategoryDTO> updateStatus(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} changing status of event category {}", userId, id);
        
        // Validar ownership
        EventCategoryDTO existing = eventCategoryService.findById(id);
        if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to change status of category {} owned by {}", userId, id, existing.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(eventCategoryService.updateStatus(id));
    }
}
