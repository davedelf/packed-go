package com.packed_go.event_service.services.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.packed_go.event_service.dtos.qr.FindTicketByCodeRequest;
import com.packed_go.event_service.dtos.qr.FindTicketByCodeResponse;
import com.packed_go.event_service.dtos.qr.ValidateConsumptionQRRequest;
import com.packed_go.event_service.dtos.qr.ValidateConsumptionQRResponse;
import com.packed_go.event_service.dtos.qr.ValidateEntryQRRequest;
import com.packed_go.event_service.dtos.qr.ValidateEntryQRResponse;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.RedeemTicketDetailDTO;
import com.packed_go.event_service.entities.Event;
import com.packed_go.event_service.entities.Pass;
import com.packed_go.event_service.entities.Ticket;
import com.packed_go.event_service.entities.TicketConsumption;
import com.packed_go.event_service.entities.TicketConsumptionDetail;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.repositories.PassRepository;
import com.packed_go.event_service.repositories.TicketConsumptionDetailRepository;
import com.packed_go.event_service.repositories.TicketConsumptionRepository;
import com.packed_go.event_service.repositories.TicketRepository;
import com.packed_go.event_service.services.QRValidationService;
import com.packed_go.event_service.services.TicketConsumptionDetailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRValidationServiceImpl implements QRValidationService {

    private final TicketRepository ticketRepository;
    private final PassRepository passRepository;
    private final EventRepository eventRepository;
    private final TicketConsumptionDetailRepository detailRepository;
    private final TicketConsumptionDetailService detailService;
    private final TicketConsumptionRepository ticketConsumptionRepository;

    // Formato QR único: PACKEDGO|T:ticketId|TC:ticketConsumptionId|E:eventId|U:userId|TS:timestamp
    // TC es opcional para entradas
    
    private Map<String, String> parseQR(String qrCode) {
        if (qrCode == null || !qrCode.startsWith("PACKEDGO|")) {
            return null;
        }
        Map<String, String> values = new HashMap<>();
        String[] parts = qrCode.split("\\|");
        for (String part : parts) {
            if (part.equals("PACKEDGO")) continue;
            int idx = part.indexOf(':');
            if (idx > 0) {
                String key = part.substring(0, idx);
                String value = part.substring(idx + 1);
                values.put(key, value);
            }
        }
        return values;
    }

    @Override
    @Transactional
    public ValidateEntryQRResponse validateEntryQR(ValidateEntryQRRequest request) {
        try {
            log.info("🎫 Validating entry QR: {}", request.getQrCode());

            // 1. Parsear QR code
            Map<String, String> qrData = parseQR(request.getQrCode());
            if (qrData == null || !qrData.containsKey("T") || !qrData.containsKey("E") || !qrData.containsKey("U")) {
                log.warn("❌ QR parsing failed or missing keys");
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Código QR inválido")
                        .build();
            }

            Long ticketId = Long.parseLong(qrData.get("T"));
            Long eventIdFromQR = Long.parseLong(qrData.get("E"));
            Long userId = Long.parseLong(qrData.get("U"));

            // 2. Validar que el evento del QR coincide con el evento solicitado
            if (!eventIdFromQR.equals(request.getEventId())) {
                log.warn("❌ Event mismatch: QR Event={} vs Request Event={}", eventIdFromQR, request.getEventId());
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Este ticket no corresponde a este evento")
                        .build();
            }

            // 3. Buscar el ticket con pass eager loaded
            Optional<Ticket> ticketOpt = ticketRepository.findByIdWithPass(ticketId);
            if (ticketOpt.isEmpty()) {
                log.warn("❌ Ticket not found: {}", ticketId);
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Ticket no encontrado")
                        .build();
            }

            Ticket ticket = ticketOpt.get();

            // 4. Verificar que el ticket esté activo
            if (!ticket.isActive()) {
                log.warn("❌ Ticket inactive: {}", ticketId);
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Ticket inactivo")
                        .build();
            }

            // 5. Verificar que el ticket pertenece al evento
            if (!ticket.getPass().getEvent().getId().equals(request.getEventId())) {
                log.warn("❌ Ticket event mismatch: Ticket Event={} vs Request Event={}", ticket.getPass().getEvent().getId(), request.getEventId());
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Ticket no válido para este evento")
                        .build();
            }

            Event event = ticket.getPass().getEvent();

            // 6. Verificar que el evento esté activo (no desactivado)
            if (!event.isActive()) {
                log.warn("❌ Event is inactive (deactivated): {}", event.getId());
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Este evento ha sido desactivado. Las entradas ya no son válidas.")
                        .build();
            }

            // 7. Verificar si ya fue usado (Single Entry)
            if (ticket.isRedeemed()) {
                log.warn("❌ Ticket already redeemed: {}", ticketId);
                return ValidateEntryQRResponse.builder()
                        .valid(false)
                        .message("❌ Entrada ya utilizada")
                        .ticketInfo(ValidateEntryQRResponse.TicketEntryInfo.builder()
                                .ticketId(ticket.getId())
                                .userId(ticket.getUserId())
                                .customerName("Usuario " + userId)
                                .eventName(event.getName())
                                .passType(getPassCodeSuffix(ticket.getPass().getCode()))
                                .alreadyUsed(true)
                                .build())
                        .build();
            }

            // 8. Marcar como usado
            ticket.setRedeemed(true);
            ticket.setRedeemedAt(java.time.LocalDateTime.now());
            ticketRepository.save(ticket);

            // 9. Construir respuesta exitosa
            log.info("✅ Ticket valid and redeemed: {}", ticketId);

            return ValidateEntryQRResponse.builder()
                    .valid(true)
                    .message("✅ Entrada autorizada")
                    .ticketInfo(ValidateEntryQRResponse.TicketEntryInfo.builder()
                            .ticketId(ticket.getId())
                            .userId(ticket.getUserId())
                            .customerName("Usuario " + userId) // TODO: Integrar con users-service
                            .eventName(event.getName())
                            .passType(getPassCodeSuffix(ticket.getPass().getCode())) // Last 8 characters
                            .alreadyUsed(false)
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error validating entry QR", e);
            return ValidateEntryQRResponse.builder()
                    .valid(false)
                    .message("❌ Error al validar el código QR: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public ValidateConsumptionQRResponse validateConsumptionQR(ValidateConsumptionQRRequest request) {
        try {
            log.info("🍺 Validating consumption QR: {}", request.getQrCode());

            // 1. Parsear QR code para obtener ticketConsumptionId
            Map<String, String> qrData = parseQR(request.getQrCode());
            if (qrData == null || !qrData.containsKey("T") || !qrData.containsKey("E")) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Código QR inválido")
                        .build();
            }

            Long ticketId = Long.parseLong(qrData.get("T"));
            Long eventIdFromQR = Long.parseLong(qrData.get("E"));

            // 2. Validar que el evento del QR coincide
            if (!eventIdFromQR.equals(request.getEventId())) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Este ticket no corresponde a este evento")
                        .build();
            }

            // 3. NOTA: Este endpoint ahora requiere que el frontend envíe también el detailId
            // en el request después de que el empleado seleccione qué consumición canjear
            // El QR solo sirve para obtener el ticketConsumptionId
            if (request.getDetailId() == null) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Debe especificar qué consumición desea canjear")
                        .build();
            }

            Long detailId = request.getDetailId();

            // 3. Buscar el detalle de consumición
            Optional<TicketConsumptionDetail> detailOpt = detailRepository.findById(detailId);
            if (detailOpt.isEmpty()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Consumición no encontrada")
                        .build();
            }

            TicketConsumptionDetail detail = detailOpt.get();

            // 4. Verificar que el evento esté activo (no desactivado)
            Event event = getEventFromDetail(detail);
            if (event != null && !event.isActive()) {
                log.warn("❌ Event is inactive (deactivated): {}", event.getId());
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Este evento ha sido desactivado. Las consumiciones ya no son válidas.")
                        .build();
            }

            // 5. Verificar que el detalle esté activo
            if (!detail.isActive()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Consumición inactiva")
                        .build();
            }

            // 6. Verificar que no esté completamente canjeado
            if (detail.isRedeem()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Esta consumición ya fue canjeada completamente")
                        .build();
            }

            // 7. Determinar cantidad a canjear
            Integer quantityToRedeem = request.getQuantity() != null ? request.getQuantity() : 1;

            if (quantityToRedeem > detail.getQuantity()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ Cantidad solicitada (" + quantityToRedeem + ") excede la disponible (" + detail.getQuantity() + ")")
                        .build();
            }

            // 7.5. 🔒 VALIDACIÓN: Verificar que la entrada haya sido canjeada primero
            Optional<Ticket> entryTicketOpt = ticketRepository.findByTicketConsumption_Id(detail.getTicketConsumption().getId());
            if (entryTicketOpt.isEmpty()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("❌ No se encontró el ticket de entrada asociado")
                        .build();
            }
            
            Ticket entryTicket = entryTicketOpt.get();
            if (!entryTicket.isRedeemed()) {
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message("Primero debe canjear la entrada")
                        .build();
            }

            // 8. Canjear la consumición (parcial o total)
            try {
                if (quantityToRedeem.equals(detail.getQuantity())) {
                    // Canje total
                    RedeemTicketDetailDTO redeemResult = detailService.redeemDetail(detailId);
                    
                    // Verificar si el canje fue exitoso
                    if (!redeemResult.isSuccess()) {
                        return ValidateConsumptionQRResponse.builder()
                                .success(false)
                                .message(redeemResult.getMessage())
                                .build();
                    }

                    return ValidateConsumptionQRResponse.builder()
                            .success(true)
                            .message("✅ Consumición canjeada exitosamente")
                            .consumptionInfo(ValidateConsumptionQRResponse.ConsumptionRedeemInfo.builder()
                                    .detailId(detail.getId())
                                    .consumptionId(detail.getConsumption().getId())
                                    .consumptionName(detail.getConsumption().getName())
                                    .consumptionType(detail.getConsumption().getCategory() != null ?
                                        detail.getConsumption().getCategory().getName() : "Sin categoría")
                                    .quantityRedeemed(quantityToRedeem)
                                    .remainingQuantity(0)
                                    .fullyRedeemed(true)
                                    .eventName(getEventNameFromDetail(detail))
                                    .build())
                            .build();
                } else {
                    // Canje parcial
                    var updatedDetail = detailService.redeemDetailPartial(detailId, quantityToRedeem);

                    Integer originalQuantity = detail.getQuantity() + quantityToRedeem;
                    return ValidateConsumptionQRResponse.builder()
                            .success(true)
                            .message("✅ Consumición parcial canjeada (" + quantityToRedeem + " de " + originalQuantity + ")")
                        .consumptionInfo(ValidateConsumptionQRResponse.ConsumptionRedeemInfo.builder()
                                .detailId(detail.getId())
                                .consumptionId(detail.getConsumption().getId())
                                .consumptionName(detail.getConsumption().getName())
                                .consumptionType(detail.getConsumption().getCategory() != null ?
                                    detail.getConsumption().getCategory().getName() : "Sin categoría")
                                .quantityRedeemed(quantityToRedeem)
                                .remainingQuantity(updatedDetail.getQuantity())
                                .fullyRedeemed(updatedDetail.isRedeem())
                                .eventName(getEventNameFromDetail(detail))
                                .build())
                        .build();
                }
            } catch (RuntimeException redeemEx) {
                // Capturar errores específicos del canje (ej: entrada no canjeada)
                log.warn("❌ Error al canjear consumición: {}", redeemEx.getMessage());
                return ValidateConsumptionQRResponse.builder()
                        .success(false)
                        .message(redeemEx.getMessage())
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Error validating consumption QR", e);
            return ValidateConsumptionQRResponse.builder()
                    .success(false)
                    .message("❌ Error al validar el código QR: " + e.getMessage())
                    .build();
        }
    }

    private String getEventNameFromDetail(TicketConsumptionDetail detail) {
        try {
            // Obtener el ticket padre para llegar al evento
            TicketConsumption ticketConsumption = detail.getTicketConsumption();
            Optional<Ticket> ticketOpt = ticketRepository.findByTicketConsumption(ticketConsumption);

            if (ticketOpt.isPresent()) {
                return ticketOpt.get().getPass().getEvent().getName();
            }
            return "Evento desconocido";
        } catch (Exception e) {
            log.warn("Could not retrieve event name for detail {}", detail.getId());
            return "Evento desconocido";
        }
    }

    private Event getEventFromDetail(TicketConsumptionDetail detail) {
        try {
            // Obtener el ticket padre para llegar al evento
            TicketConsumption ticketConsumption = detail.getTicketConsumption();
            Optional<Ticket> ticketOpt = ticketRepository.findByTicketConsumption(ticketConsumption);

            if (ticketOpt.isPresent()) {
                return ticketOpt.get().getPass().getEvent();
            }
            return null;
        } catch (Exception e) {
            log.warn("Could not retrieve event for detail {}", detail.getId());
            return null;
        }
    }

    @Override
    public FindTicketByCodeResponse findTicketByCode(FindTicketByCodeRequest request) {
        try {
            log.info("🔍 Searching ticket with code suffix: {} for event: {}", request.getCode(), request.getEventId());

            // 1. Buscar passes que terminen con el código proporcionado
            List<Pass> matchingPasses = passRepository.findByCodeSuffix(request.getCode());

            if (matchingPasses.isEmpty()) {
                log.warn("❌ No passes found with code suffix: {}", request.getCode());
                return null;
            }

            // 2. Filtrar por evento y buscar el ticket correspondiente
            for (Pass pass : matchingPasses) {
                // Verificar que el pass pertenece al evento solicitado
                if (!pass.getEvent().getId().equals(request.getEventId())) {
                    continue;
                }

                // Buscar el ticket asociado a este pass
                Optional<Ticket> ticketOpt = ticketRepository.findByPass_Id(pass.getId());
                
                if (ticketOpt.isEmpty()) {
                    continue;
                }

                Ticket ticket = ticketOpt.get();

                // Verificar que el ticket esté activo
                if (!ticket.isActive()) {
                    continue;
                }

                // 3. Construir el código QR del ticket (usar texto para validación, no imagen)
                String qrCode = ticket.getQrText();

                log.info("✅ Ticket found: ID={}, Pass={}", ticket.getId(), pass.getCode());

                // 4. Retornar la respuesta
                return FindTicketByCodeResponse.builder()
                        .ticketId(ticket.getId())
                        .qrCode(qrCode)
                        .passCode(pass.getCode())
                        .eventId(pass.getEvent().getId())
                        .eventName(pass.getEvent().getName())
                        .userId(ticket.getUserId())
                        .build();
            }

            log.warn("❌ No active ticket found with code suffix {} for event {}", request.getCode(), request.getEventId());
            return null;

        } catch (Exception e) {
            log.error("❌ Error finding ticket by code", e);
            return null;
        }
    }

    /**
     * Helper method to get the last 8 characters of the pass code
     * @param passCode Full pass code
     * @return Last 8 characters or full code if shorter
     */
    private String getPassCodeSuffix(String passCode) {
        if (passCode == null) {
            return "N/A";
        }
        if (passCode.length() <= 8) {
            return passCode;
        }
        return passCode.substring(passCode.length() - 8);
    }
}
