package com.packed_go.event_service.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionWithDetailsDTO;
import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionWithSimpleDetailsDTO;
import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.ConcurrencyTestDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.RedeemTicketDetailDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.services.TicketConsumptionDetailService;
import com.packed_go.event_service.services.TicketConsumptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/event-service/ticket-consumption")
@RequiredArgsConstructor
@Slf4j
public class TicketConsumptionController {
    private final TicketConsumptionService ticketService;
    private final TicketConsumptionDetailService ticketDetailService;

    @PostMapping
    public ResponseEntity<TicketConsumptionDTO> createTicket(@RequestBody TicketConsumptionDTO dto) {
        TicketConsumptionDTO savedTicket = ticketService.create(dto);
        return ResponseEntity.ok(savedTicket);
    }

    @PostMapping("/from-consumptions")
    public ResponseEntity<TicketConsumptionDTO> createTicketFromConsumptions(@RequestBody CreateTicketConsumptionDTO dto) {
        TicketConsumptionDTO savedTicket = ticketService.createFromConsumptions(dto);
        return ResponseEntity.ok(savedTicket);
    }

    @PostMapping("/with-details")
    public ResponseEntity<TicketConsumptionDTO> createTicketWithDetails(@RequestBody CreateTicketConsumptionWithDetailsDTO dto) {
        TicketConsumptionDTO savedTicket = ticketService.createWithDetails(dto);
        return ResponseEntity.ok(savedTicket);
    }

    @PostMapping("/with-simple-details")
    public ResponseEntity<TicketConsumptionDTO> createTicketWithSimpleDetails(@RequestBody CreateTicketConsumptionWithSimpleDetailsDTO dto) {
        TicketConsumptionDTO savedTicket = ticketService.createWithSimpleDetails(dto);
        return ResponseEntity.ok(savedTicket);
    }

    @PutMapping("/detail/{detailId}/redeem")
    public ResponseEntity<RedeemTicketDetailDTO> redeemTicketDetail(@PathVariable Long detailId) {
        log.info("Canjeando detalle del ticket con ID: {}", detailId);
        RedeemTicketDetailDTO result = ticketDetailService.redeemDetail(detailId);
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint para canjear parcialmente una consumición.
     * Usado por consumption-service al validar QRs de consumición con cantidad.
     */
    @PutMapping("/detail/{detailId}/redeem-partial")
    public ResponseEntity<TicketConsumptionDetailDTO> redeemTicketDetailPartial(
            @PathVariable Long detailId,
            @RequestParam Integer quantityToRedeem) {
        log.info("Canjeando parcialmente detalle del ticket {} con cantidad: {}", detailId, quantityToRedeem);
        TicketConsumptionDetailDTO result = ticketDetailService.redeemDetailPartial(detailId, quantityToRedeem);
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint para obtener un detalle de consumición específico.
     * Usado por consumption-service al validar QRs.
     */
    @GetMapping("/detail/{detailId}")
    public ResponseEntity<TicketConsumptionDetailDTO> getTicketDetail(@PathVariable Long detailId) {
        log.info("Obteniendo detalle de consumición con ID: {}", detailId);
        TicketConsumptionDetailDTO detail = ticketDetailService.findById(detailId);
        return ResponseEntity.ok(detail);
    }

    /**
     * Endpoint para obtener todas las consumiciones de un ticket de consumición (por ID de TicketConsumption).
     */
    @GetMapping("/{ticketConsumptionId}/details")
    public ResponseEntity<List<TicketConsumptionDetailDTO>> getTicketConsumptionDetails(
            @PathVariable Long ticketConsumptionId) {
        log.info("Obteniendo detalles del ticket de consumición: {}", ticketConsumptionId);
        List<TicketConsumptionDetailDTO> details = ticketDetailService.findAllByTicketId(ticketConsumptionId);
        return ResponseEntity.ok(details);
    }

    /**
     * Endpoint para obtener todas las consumiciones de un ticket de entrada (por ID de Ticket).
     */
    @GetMapping("/by-ticket/{ticketId}/details")
    public ResponseEntity<List<TicketConsumptionDetailDTO>> getTicketDetails(@PathVariable Long ticketId) {
        log.info("Obteniendo detalles del ticket de entrada con ID: {}", ticketId);
        List<TicketConsumptionDetailDTO> details = ticketDetailService.findAllByEntryTicketId(ticketId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketConsumptionDTO> getTicket(@PathVariable Long ticketId) {
        log.info("Obteniendo ticket con ID: {}", ticketId);
        TicketConsumptionDTO ticket = ticketService.findById(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/detail/{detailId}/test-concurrency")
    public ResponseEntity<ConcurrencyTestDTO> testConcurrency(@PathVariable Long detailId, @RequestParam(defaultValue = "5") int concurrentRequests) {
        log.info("Probando concurrencia para detalle {} con {} peticiones concurrentes", detailId, concurrentRequests);
        
        long startTime = System.currentTimeMillis();
        AtomicInteger successfulRedemptions = new AtomicInteger(0);
        AtomicInteger failedRedemptions = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    RedeemTicketDetailDTO result = ticketDetailService.redeemDetail(detailId);
                    if (result.isSuccess()) {
                        successfulRedemptions.incrementAndGet();
                    } else {
                        failedRedemptions.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedRedemptions.incrementAndGet();
                    log.warn("Error en petición concurrente: {}", e.getMessage());
                }
            }, executor);
            futures.add(future);
        }
        
        // Esperar a que todas las peticiones terminen
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        ConcurrencyTestDTO result = new ConcurrencyTestDTO();
        result.setTicketDetailId(detailId);
        result.setConcurrentRequests(concurrentRequests);
        result.setSuccessfulRedemptions(successfulRedemptions.get());
        result.setFailedRedemptions(failedRedemptions.get());
        result.setExecutionTimeMs(executionTime);
        result.setTestResult("Prueba completada - Solo una petición debería ser exitosa");
        
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint para obtener estadísticas de redención por IDs de ticket consumption.
     * Retorna un mapa con el ID del ticket consumption y su estado de redención.
     */
    @PostMapping("/redemption-stats")
    public ResponseEntity<java.util.Map<Long, Boolean>> getRedemptionStats(@RequestBody List<Long> ticketConsumptionIds) {
        log.info("Obteniendo estadísticas de redención para {} ticket consumptions", ticketConsumptionIds.size());
        
        java.util.Map<Long, Boolean> stats = new java.util.HashMap<>();
        
        for (Long tcId : ticketConsumptionIds) {
            try {
                TicketConsumptionDTO ticket = ticketService.findById(tcId);
                stats.put(tcId, ticket.isRedeem());
            } catch (Exception e) {
                log.warn("No se pudo obtener ticket consumption {}: {}", tcId, e.getMessage());
                stats.put(tcId, false);
            }
        }
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Endpoint para obtener estadísticas de redención por organizador.
     * Retorna total de tickets vendidos y total de tickets canjeados.
     */
    @GetMapping("/redemption-stats/organizer/{organizerId}")
    public ResponseEntity<java.util.Map<String, Long>> getRedemptionStatsByOrganizer(@PathVariable Long organizerId) {
        log.info("📊 Obteniendo estadísticas de redención para organizador: {}", organizerId);
        java.util.Map<String, Long> stats = ticketService.getRedemptionStatsByOrganizer(organizerId);
        return ResponseEntity.ok(stats);
    }
}
