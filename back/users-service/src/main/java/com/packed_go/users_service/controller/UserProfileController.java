package com.packed_go.users_service.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.users_service.dto.UserProfileDTO;
import com.packed_go.users_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.users_service.dto.request.UpdateUserProfileRequest;
import com.packed_go.users_service.model.UserProfile;
import com.packed_go.users_service.security.JwtTokenValidator;
import com.packed_go.users_service.service.UserProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user-profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService service;
    private final ModelMapper modelMapper;
    private final JwtTokenValidator jwtValidator;

    /**
     * 🔐 Método helper para validar JWT y permisos de acceso
     * Valida que el userId del JWT coincida con el solicitado
     */
    private Long validateAndExtractUserId(String authHeader, Long requestedUserId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }
        
        Long tokenUserId = jwtValidator.getUserIdFromToken(token);
        
        if (!tokenUserId.equals(requestedUserId)) {
            throw new RuntimeException("Cannot access other user's resources");
        }
        
        return tokenUserId;
    }


    /**
     * 🔒 GET /user-profiles - Obtener todos los perfiles (solo ADMIN)
     * Endpoint para dashboard de administrador
     */
    @GetMapping
    public ResponseEntity<List<UserProfileDTO>> getAllProfiles(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("🔒 Getting all user profiles");
        
        // Validar JWT
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }
        
        // Validar que sea ADMIN
        String role = jwtValidator.getRoleFromToken(token);
        if (!"ADMIN".equals(role)) {
            log.warn("⚠️ Non-admin user attempted to access all profiles");
            throw new RuntimeException("Access denied: Admin role required");
        }
        
        List<UserProfile> profiles = service.getAll();
        List<UserProfileDTO> dtos = profiles.stream()
                .map(profile -> modelMapper.map(profile, UserProfileDTO.class))
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<UserProfileDTO> create(@RequestBody UserProfileDTO dto) {
        UserProfile created = service.create(modelMapper.map(dto,UserProfile.class));
        if (created != null) {
            return ResponseEntity.ok(modelMapper.map(created,UserProfileDTO.class));
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @PostMapping("/from-auth")
    public ResponseEntity<UserProfileDTO> createFromAuthService(@RequestBody CreateProfileFromAuthRequest request) {
        try {
            log.info("Creating profile from auth-service for authUserId: {}", request.getAuthUserId());
            
            UserProfile created = service.createFromAuthService(request);
            
            log.info("Successfully created profile with ID: {} for authUserId: {}", 
                    created.getId(), request.getAuthUserId());
            
            return ResponseEntity.ok(modelMapper.map(created, UserProfileDTO.class));
            
        } catch (RuntimeException e) {
            log.warn("Failed to create profile from auth-service: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error creating profile from auth-service", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * 🔒 GET /user-profiles/{authUserId} - Obtener perfil por authUserId (valida ownership)
     * Solo el owner puede acceder a su propio perfil
     * NOTA: Este endpoint ahora usa authUserId en lugar de id autoincremental
     */
    @GetMapping("/{authUserId}")
    public ResponseEntity<UserProfileDTO> getById(
            @PathVariable Long authUserId,
            @RequestHeader("Authorization") String authHeader) {

        log.info("🔒 Getting profile for authUserId {}", authUserId);

        // Validar JWT
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }

        // Validar ownership o rol de ADMIN usando el helper
        if (!jwtValidator.canAccessUserResources(token, authUserId)) {
            Long tokenUserId = jwtValidator.getUserIdFromToken(token);
            log.warn("⚠️ User {} attempted to access profile of user {}",
                    tokenUserId, authUserId);
            throw new RuntimeException("Access Denied");
        }

        UserProfile profile = service.getByAuthUserId(authUserId);

        return ResponseEntity.ok(modelMapper.map(profile, UserProfileDTO.class));
    }


    /**
     * 🔒 GET /user-profiles/my-profile - Obtener perfil del usuario autenticado
     * Reemplaza el endpoint público GET /user-profiles
     */
    @GetMapping("/my-profile")
    public ResponseEntity<UserProfileDTO> getMyProfile(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("🔒 Getting my profile");
        
        // Validar JWT y extraer userId
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }
        
        Long userId = jwtValidator.getUserIdFromToken(token);
        UserProfile profile = service.getByAuthUserId(userId);
        
        return ResponseEntity.ok(modelMapper.map(profile, UserProfileDTO.class));
    }


    /**
     * 🔒 PUT /user-profiles/{authUserId} - Actualizar perfil (valida ownership)
     * Solo el owner puede actualizar su propio perfil
     * NOTA: Este endpoint ahora usa authUserId en lugar de id autoincremental
     */
    @PutMapping("/{authUserId}")
    public ResponseEntity<UserProfileDTO> update(
            @PathVariable Long authUserId,
            @Valid @RequestBody UpdateUserProfileRequest request,
            @RequestHeader("Authorization") String authHeader) {

        log.info("🔒 Updating profile for authUserId {}", authUserId);

        // Validar JWT
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }

        Long tokenUserId = jwtValidator.getUserIdFromToken(token);

        // Validar ownership
        if (!authUserId.equals(tokenUserId)) {
            log.warn("⚠️ User {} attempted to update profile of user {}",
                    tokenUserId, authUserId);
            throw new RuntimeException("Access denied: You can only update your own profile");
        }

        try {
            UserProfile updated = service.updateByAuthUserId(authUserId, request);
            return ResponseEntity.ok(modelMapper.map(updated, UserProfileDTO.class));
        } catch (RuntimeException e) {
            log.warn("Failed to update profile for authUserId {}: {}", authUserId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


    /**
     * 🔒 DELETE /user-profiles/{authUserId} - Eliminar perfil físicamente (valida ownership)
     * Solo el owner puede eliminar su propio perfil
     * NOTA: Este endpoint ahora usa authUserId en lugar de id autoincremental
     */
    @DeleteMapping("/{authUserId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long authUserId,
            @RequestHeader("Authorization") String authHeader) {

        log.info("🔒 Deleting profile for authUserId {}", authUserId);

        // Validar JWT
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }

        Long tokenUserId = jwtValidator.getUserIdFromToken(token);

        // Validar ownership
        if (!authUserId.equals(tokenUserId)) {
            log.warn("⚠️ User {} attempted to delete profile of user {}",
                    tokenUserId, authUserId);
            throw new RuntimeException("Access denied: You can only delete your own profile");
        }

        // Obtener el perfil para conseguir el id interno
        UserProfile existingProfile = service.getByAuthUserId(authUserId);
        service.delete(existingProfile.getId());

        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/logical/{id}")
    public ResponseEntity<UserProfileDTO> deleteLogical(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(service.deleteLogical(id),UserProfileDTO.class));
    }


    @GetMapping("/active")
    public ResponseEntity<List<UserProfile>> getAllActive() {
        return ResponseEntity.ok(service.getAllActive());
    }


//    @GetMapping("/active/email/{email}")
//    public ResponseEntity<UserProfile> getByEmailActive(@PathVariable String email) {
//        return ResponseEntity.ok(service.getByEmailActive(email));
//    }


    @GetMapping("/active/{id}")
    public ResponseEntity<UserProfile> getByIdActive(@PathVariable Long id) {
        return ResponseEntity.ok(service.getByIdActive(id));
    }


    @GetMapping("/active/document/{document}")
    public ResponseEntity<UserProfile> getByDocumentActive(@PathVariable Long document) {
        return ResponseEntity.ok(service.getByDocumentActive(document));
    }

    @GetMapping("/by-auth-user/{authUserId}")
    public ResponseEntity<UserProfileDTO> getByAuthUserId(
            @PathVariable Long authUserId,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("Getting user profile by authUserId: {}", authUserId);
        
        // 🔐 Validación JWT simple
        validateAndExtractUserId(authHeader, authUserId);
        
        UserProfile profile = service.getByAuthUserId(authUserId);
        return ResponseEntity.ok(modelMapper.map(profile, UserProfileDTO.class));
    }

    @GetMapping("/active/by-auth-user/{authUserId}")
    public ResponseEntity<UserProfileDTO> getByAuthUserIdActive(
            @PathVariable Long authUserId,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("Getting active user profile by authUserId: {}", authUserId);
        
        // 🔐 Validación JWT simple
        validateAndExtractUserId(authHeader, authUserId);
        
        UserProfile profile = service.getByAuthUserIdActive(authUserId);
        return ResponseEntity.ok(modelMapper.map(profile, UserProfileDTO.class));
    }

    @PutMapping("/by-auth-user/{authUserId}")
    public ResponseEntity<UserProfileDTO> updateByAuthUserId(
            @PathVariable Long authUserId, 
            @Valid @RequestBody UpdateUserProfileRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("Updating user profile by authUserId: {}", authUserId);
        
        // 🔐 Validación JWT simple
        validateAndExtractUserId(authHeader, authUserId);
        
        try {
            UserProfile updated = service.updateByAuthUserId(authUserId, request);
            return ResponseEntity.ok(modelMapper.map(updated, UserProfileDTO.class));
        } catch (RuntimeException e) {
            log.warn("Failed to update profile for authUserId {}: {}", authUserId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

}
