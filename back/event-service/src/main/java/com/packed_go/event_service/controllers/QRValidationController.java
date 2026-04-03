package com.packed_go.event_service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.event_service.dtos.qr.FindTicketByCodeRequest;
import com.packed_go.event_service.dtos.qr.FindTicketByCodeResponse;
import com.packed_go.event_service.dtos.qr.ValidateConsumptionQRRequest;
import com.packed_go.event_service.dtos.qr.ValidateConsumptionQRResponse;
import com.packed_go.event_service.dtos.qr.ValidateEntryQRRequest;
import com.packed_go.event_service.dtos.qr.ValidateEntryQRResponse;
import com.packed_go.event_service.services.QRValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller para validar QR codes de entrada y consumiciones
 * Usado por empleados desde users-service
 */
@RestController
@RequestMapping("/event-service/qr-validation")
@RequiredArgsConstructor
@Slf4j
public class QRValidationController {

    private final QRValidationService qrValidationService;

    /**
     * 🔓 POST /qr-validation/validate-entry
     * Valida un QR code de entrada al evento (Pass/Ticket)
     */
    @PostMapping("/validate-entry")
    public ResponseEntity<ValidateEntryQRResponse> validateEntry(@RequestBody ValidateEntryQRRequest request) {
        log.info("🎫 Validating entry QR for event: {}", request.getEventId());
        ValidateEntryQRResponse response = qrValidationService.validateEntryQR(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 🔓 POST /qr-validation/validate-consumption
     * Valida y canjea un QR code de consumición
     */
    @PostMapping("/validate-consumption")
    public ResponseEntity<ValidateConsumptionQRResponse> validateConsumption(@RequestBody ValidateConsumptionQRRequest request) {
        log.info("🍺 Validating consumption QR for event: {}", request.getEventId());
        ValidateConsumptionQRResponse response = qrValidationService.validateConsumptionQR(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 🔓 POST /qr-validation/find-by-code
     * Busca un ticket por los últimos 8 caracteres del código
     */
    @PostMapping("/find-by-code")
    public ResponseEntity<FindTicketByCodeResponse> findByCode(@RequestBody FindTicketByCodeRequest request) {
        log.info("🔍 Finding ticket by code: {} for event: {}", request.getCode(), request.getEventId());
        FindTicketByCodeResponse response = qrValidationService.findTicketByCode(request);
        
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }
}
