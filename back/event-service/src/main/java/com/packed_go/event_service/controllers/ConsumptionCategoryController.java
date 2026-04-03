package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.consumptionCategory.ConsumptionCategoryDTO;
import com.packed_go.event_service.dtos.consumptionCategory.CreateConsumptionCategoryDTO;
import com.packed_go.event_service.security.JwtTokenValidator;
import com.packed_go.event_service.services.ConsumptionCategoryService;
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
@RequestMapping("/event-service/consumption-category")
@RequiredArgsConstructor
public class ConsumptionCategoryController {
    private static final Logger log = LoggerFactory.getLogger(ConsumptionCategoryController.class);
    private final ConsumptionCategoryService consumptionCategoryService;
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
     * 🔒 POST /consumption-category - Crear categoría de consumición (solo admins)
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody CreateConsumptionCategoryDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            log.info("🔒 User {} creating consumption category: {}", userId, dto.getName());
            
            // Inyectar createdBy desde JWT
            if (dto.getCreatedBy() == null) {
                dto.setCreatedBy(userId);
            } else if (!dto.getCreatedBy().equals(userId)) {
                log.warn("⚠️ User {} tried to create category with different createdBy: {}", userId, dto.getCreatedBy());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            ConsumptionCategoryDTO created = consumptionCategoryService.create(dto);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsumptionCategoryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(consumptionCategoryService.findById(id));
    }

    /**
     * 🔒 GET /consumption-category - Listar categorías (admins ven solo las suyas, customers ven todas)
     */
    @GetMapping
    public ResponseEntity<List<ConsumptionCategoryDTO>> getAll(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Si no hay token, devolver todas (para público)
        if (authHeader == null || authHeader.isEmpty()) {
            return ResponseEntity.ok(consumptionCategoryService.findAll());
        }
        
        try {
            Long userId = extractUserIdFromToken(authHeader);
            log.info("🔒 User {} fetching consumption categories", userId);
            
            List<ConsumptionCategoryDTO> categories = consumptionCategoryService.findAll().stream()
                    .filter(cat -> cat.getCreatedBy() != null && cat.getCreatedBy().equals(userId))
                    .toList();
            
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            // Si falla la validación del token, devolver todas
            return ResponseEntity.ok(consumptionCategoryService.findAll());
        }
    }

    /**
     * � GET /consumption-category/active - Categorías activas del admin (multitenant)
     */
    @GetMapping("/active")
    public ResponseEntity<List<ConsumptionCategoryDTO>> getAllActive(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} fetching active consumption categories", userId);
        return ResponseEntity.ok(consumptionCategoryService.findByActiveIsTrueAndCreatedBy(userId));
    }

    /**
     * 🔒 PUT /consumption-category/{id} - Actualizar categoría (valida ownership)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConsumptionCategoryDTO> update(
            @PathVariable Long id, 
            @RequestBody CreateConsumptionCategoryDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} updating consumption category {}", userId, id);
        
        // Validar ownership
        ConsumptionCategoryDTO existing = consumptionCategoryService.findById(id);
        if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to update category {} owned by {}", userId, id, existing.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(consumptionCategoryService.update(id, dto));
    }

    /**
     * 🔒 DELETE /consumption-category/{id} - Eliminar categoría (valida ownership)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} deleting consumption category {}", userId, id);
        
        // Validar ownership
        ConsumptionCategoryDTO existing = consumptionCategoryService.findById(id);
        if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to delete category {} owned by {}", userId, id, existing.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        consumptionCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 🔒 DELETE /consumption-category/logical/{id} - Desactivar categoría (valida ownership)
     */
    @DeleteMapping("/logical/{id}")
    public ResponseEntity<ConsumptionCategoryDTO> deleteLogical(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} deactivating consumption category {}", userId, id);
        
        // Validar ownership
        ConsumptionCategoryDTO existing = consumptionCategoryService.findById(id);
        if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to deactivate category {} owned by {}", userId, id, existing.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(modelMapper.map(consumptionCategoryService.deleteLogical(id), ConsumptionCategoryDTO.class));
    }

    /**
     * 🔒 PUT /consumption-category/status/{id} - Cambiar estado de categoría (valida ownership)
     */
    @PutMapping("/status/{id}")
    public ResponseEntity<ConsumptionCategoryDTO> updateStatus(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = extractUserIdFromToken(authHeader);
        log.info("🔒 User {} changing status of consumption category {}", userId, id);
        
        // Validar ownership
        ConsumptionCategoryDTO existing = consumptionCategoryService.findById(id);
        if (existing.getCreatedBy() != null && !existing.getCreatedBy().equals(userId)) {
            log.warn("⚠️ User {} tried to change status of category {} owned by {}", userId, id, existing.getCreatedBy());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(consumptionCategoryService.updateStatus(id));
    }
}
