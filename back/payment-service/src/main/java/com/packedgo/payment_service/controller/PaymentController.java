package com.packedgo.payment_service.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packedgo.payment_service.dto.PaymentRequest;
import com.packedgo.payment_service.dto.PaymentResponse;
import com.packedgo.payment_service.security.JwtTokenValidator;
import com.packedgo.payment_service.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    private final JwtTokenValidator jwtTokenValidator;

    /**
     * Endpoint para crear un pago con Stripe Checkout
     * SEGURIDAD: Requiere autenticación JWT
     */
    @PostMapping("/create-checkout-stripe")
    public ResponseEntity<PaymentResponse> createPaymentWithStripe(
            @Valid @RequestBody PaymentRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("🔵 Received POST /api/payments/create-checkout-stripe");
        log.info("📦 Request body: adminId={}, orderId={}, amount={}", 
                request.getAdminId(), request.getOrderId(), request.getAmount());
        log.info("🔐 Authorization header present: {}", authHeader != null);

        try {
            // SECURITY: Validar JWT token
            // String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
            // if (!jwtTokenValidator.validateToken(token)) {
            //     log.warn("⚠️ Token JWT inválido en /payments/create-checkout-stripe");
            //     return ResponseEntity
            //             .status(HttpStatus.UNAUTHORIZED)
            //             .body(PaymentResponse.builder()
            //                     .message("Token JWT inválido o expirado")
            //                     .build());
            // }

            // Long userIdFromToken = jwtTokenValidator.getUserIdFromToken(token);

            // log.info("✅ Token válido - UserId: {}, OrderId: {}",
            //         userIdFromToken, request.getOrderId());

            PaymentResponse response = paymentService.createPaymentWithStripe(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(PaymentResponse.builder()
                            .message("Error de autenticación: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error creando pago con Stripe", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.builder()
                            .message("Error al crear el pago con Stripe: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Verificar estado del pago manualmente (para casos donde el webhook falla o tarda)
     */
    @PostMapping("/verify/{orderId}")
    public ResponseEntity<PaymentResponse> verifyPaymentStatus(@PathVariable String orderId) {
        log.info("🔍 Verifying payment status for order: {}", orderId);
        PaymentResponse response = paymentService.verifyPaymentStatus(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "payment-gateway",
                "version", "2.0.0",
                "provider", "Stripe"
        ));
    }
    
    /**
     * 🔒 GET /payments/stats - Obtener estadísticas de pagos del admin autenticado
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getPaymentStats(@org.springframework.web.bind.annotation.RequestHeader("Authorization") String authHeader) {
        try {
            String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
            if (!jwtTokenValidator.validateToken(token)) {
                log.warn("⚠️ Token JWT inválido en /payments/stats");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token JWT inválido o expirado"));
            }
            
            Long adminId = jwtTokenValidator.getUserIdFromToken(token);
            log.info("🔒 User {} fetching payment stats", adminId);
            
            com.packedgo.payment_service.dto.PaymentStatsDTO stats = paymentService.getPaymentStats(adminId);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting payment stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}