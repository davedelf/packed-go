package com.packed_go.analytics_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.analytics_service.dto.DashboardDTO;
import com.packed_go.analytics_service.security.JwtTokenValidator;
import com.packed_go.analytics_service.service.AnalyticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador REST para Analytics
 * Endpoint: /api/dashboard (context-path ya incluye /api)
 */
@RestController
@RequestMapping("/dashboard")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final JwtTokenValidator jwtTokenValidator;

    /**
     * GET /api/dashboard
     * Obtiene el dashboard completo de analytics para el organizador autenticado
     */
    @GetMapping
    public ResponseEntity<DashboardDTO> getDashboard(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        try {
            log.info("📊 Solicitud de dashboard recibida");

            // Extraer y validar token
            String token = jwtTokenValidator.extractTokenFromHeader(authorizationHeader);
            Long organizerId = jwtTokenValidator.getUserIdFromToken(token);
            String role = jwtTokenValidator.getRoleFromToken(token);

            log.info("👤 Organizador ID: {} | Role: {}", organizerId, role);

            // Verificar que sea ADMIN
            if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
                log.warn("⚠️ Usuario no autorizado. Solo ADMIN puede acceder al dashboard");
                return ResponseEntity.status(403).build();
            }

            // Generar dashboard
            DashboardDTO dashboard = analyticsService.generateDashboard(organizerId, token);

            log.info("✅ Dashboard generado exitosamente para organizador {}", organizerId);
            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Error generando dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /api/dashboard/{organizerId}
     * Obtiene el dashboard de un organizador específico (solo SUPER_ADMIN)
     */
    @GetMapping("/{organizerId}")
    public ResponseEntity<DashboardDTO> getDashboardForOrganizer(
            @PathVariable Long organizerId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        try {
            log.info("📊 Solicitud de dashboard para organizador: {}", organizerId);

            // Extraer y validar token
            String token = jwtTokenValidator.extractTokenFromHeader(authorizationHeader);
            Long requesterId = jwtTokenValidator.getUserIdFromToken(token);
            String role = jwtTokenValidator.getRoleFromToken(token);

            log.info("👤 Requester ID: {} | Role: {}", requesterId, role);

            // Verificar permisos
            if (!"SUPER_ADMIN".equals(role) && !requesterId.equals(organizerId)) {
                log.warn("⚠️ Usuario no autorizado. Solo SUPER_ADMIN puede ver dashboards de otros");
                return ResponseEntity.status(403).build();
            }

            // Generar dashboard
            DashboardDTO dashboard = analyticsService.generateDashboard(organizerId, token);

            log.info("✅ Dashboard generado exitosamente para organizador {}", organizerId);
            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            log.error("❌ Error generando dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /api/dashboard/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Analytics Service is UP");
    }
}
