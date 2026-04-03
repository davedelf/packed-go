package com.packed_go.event_service.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.packed_go.event_service.dtos.pass.PassDTO;
import com.packed_go.event_service.dtos.ticket.ConsumptionItemDTO;
import com.packed_go.event_service.dtos.ticket.CreateTicketDTO;
import com.packed_go.event_service.dtos.ticket.CreateTicketWithConsumptionsRequest;
import com.packed_go.event_service.dtos.ticket.TicketDTO;
import com.packed_go.event_service.dtos.ticket.TicketWithConsumptionsResponse;
import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.entities.Event;
import com.packed_go.event_service.entities.Pass;
import com.packed_go.event_service.entities.Ticket;
import com.packed_go.event_service.entities.TicketConsumption;
import com.packed_go.event_service.entities.TicketConsumptionDetail;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.repositories.PassRepository;
import com.packed_go.event_service.repositories.TicketConsumptionDetailRepository;
import com.packed_go.event_service.repositories.TicketConsumptionRepository;
import com.packed_go.event_service.repositories.TicketRepository;
import com.packed_go.event_service.services.QRCodeService;
import com.packed_go.event_service.services.TicketConsumptionService;
import com.packed_go.event_service.services.TicketService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    private final TicketRepository ticketRepository;
    private final PassRepository passRepository;
    private final TicketConsumptionRepository ticketConsumptionRepository;
    private final TicketConsumptionDetailRepository ticketConsumptionDetailRepository;
    private final ConsumptionRepository consumptionRepository;
    private final EventRepository eventRepository;
    private final TicketConsumptionService ticketConsumptionService;
    private final QRCodeService qrCodeService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TicketDTO createTicket(CreateTicketDTO createTicketDTO) {
        // Buscar el pass por código con bloqueo pesimista
        Pass pass = passRepository.findByCodeWithLock(createTicketDTO.getPassCode())
                .orElseThrow(() -> new RuntimeException("Pass with code " + createTicketDTO.getPassCode() + " not found"));

        // Verificar que el pass esté disponible
        if (!pass.isAvailable()) {
            throw new RuntimeException("Pass is not available for purchase");
        }

        // Crear el TicketConsumption
        TicketConsumptionDTO ticketConsumptionDTO = ticketConsumptionService.createWithDetails(createTicketDTO.getTicketConsumption());
        TicketConsumption ticketConsumption = modelMapper.map(ticketConsumptionDTO, TicketConsumption.class);

        // Crear el Ticket
        Ticket ticket = new Ticket(createTicketDTO.getUserId(), pass, ticketConsumption);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Marcar el pass como vendido
        pass.setSold(true);
        pass.setAvailable(false);
        pass.setSoldToUserId(createTicketDTO.getUserId());
        pass.setSoldAt(LocalDateTime.now());
        passRepository.save(pass);

        return modelMapper.map(savedTicket, TicketDTO.class);
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TicketDTO purchaseTicket(Long userId, String passCode, Long ticketConsumptionId) {
        // Buscar el pass por código con bloqueo pesimista
        Pass pass = passRepository.findByCodeWithLock(passCode)
                .orElseThrow(() -> new RuntimeException("Pass with code " + passCode + " not found"));

        // Verificar que el pass esté disponible
        if (!pass.isAvailable()) {
            throw new RuntimeException("Pass is not available for purchase");
        }

        // Buscar el TicketConsumption
        TicketConsumption ticketConsumption = ticketConsumptionRepository.findById(ticketConsumptionId)
                .orElseThrow(() -> new RuntimeException("TicketConsumption with id " + ticketConsumptionId + " not found"));

        // Crear el Ticket
        Ticket ticket = new Ticket(userId, pass, ticketConsumption);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Marcar el pass como vendido
        pass.setSold(true);
        pass.setAvailable(false);
        pass.setSoldToUserId(userId);
        pass.setSoldAt(LocalDateTime.now());
        passRepository.save(pass);

        return modelMapper.map(savedTicket, TicketDTO.class);
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TicketDTO redeemTicket(Long ticketId) {
        // Buscar el ticket con bloqueo pesimista
        Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket with id " + ticketId + " not found"));

        // Verificar que el ticket esté activo
        if (!ticket.isActive()) {
            throw new RuntimeException("Ticket is not active");
        }

        // Verificar que no esté ya canjeado
        if (ticket.isRedeemed()) {
            throw new RuntimeException("Ticket is already redeemed");
        }

        // Marcar como canjeado
        ticket.setRedeemed(true);
        ticket.setRedeemedAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(ticket);

        return modelMapper.map(savedTicket, TicketDTO.class);
    }

    @Override
    public TicketDTO findById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket with id " + id + " not found"));
        return modelMapper.map(ticket, TicketDTO.class);
    }

    @Override
    public TicketDTO findByPassCode(String passCode) {
        Ticket ticket = ticketRepository.findByPassCode(passCode)
                .orElseThrow(() -> new RuntimeException("Ticket with pass code " + passCode + " not found"));
        return modelMapper.map(ticket, TicketDTO.class);
    }

    @Override
    @Transactional
    public List<TicketDTO> findByUserId(Long userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return tickets.stream()
                .map(this::convertToDTO)
                .toList();
    }

    private TicketDTO convertToDTO(Ticket ticket) {
        log.info("Converting ticket ID {} to DTO. QR Code from entity: {}", 
                ticket.getId(), ticket.getQrCode() != null && !ticket.getQrCode().isEmpty());
        
        TicketDTO dto = new TicketDTO();
        dto.setId(ticket.getId());
        dto.setUserId(ticket.getUserId());
        dto.setActive(ticket.isActive());
        dto.setRedeemed(ticket.isRedeemed());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setPurchasedAt(ticket.getPurchasedAt());
        dto.setRedeemedAt(ticket.getRedeemedAt());
        
        // Mapear el QR code
        String qrCodeValue = ticket.getQrCode();
        log.info("QR Code value from ticket {}: {}", ticket.getId(), qrCodeValue != null ? "present (length: " + qrCodeValue.length() + ")" : "null");
        dto.setQrCode(qrCodeValue);
        
        // Mapear el pass y detalles del evento
        if (ticket.getPass() != null) {
            dto.setPass(modelMapper.map(ticket.getPass(), PassDTO.class));
            dto.setPassCode(ticket.getPass().getCode());
            log.info("Pass code set for ticket {}: {}", ticket.getId(), ticket.getPass().getCode());
            
            // Mapear detalles del evento desde el Pass
            if (ticket.getPass().getEvent() != null) {
                Event event = ticket.getPass().getEvent();
                dto.setEventId(event.getId());
                dto.setEventName(event.getName());
                dto.setEventDate(event.getEventDate());
                
                // Formatear ubicación (mantener para compatibilidad)
                String location = "";
                if (event.getLat() != null && event.getLng() != null) {
                    location = event.getLat() + ", " + event.getLng();
                }
                dto.setEventLocation(location);
                
                // Mapear campos individuales de ubicación para Google Maps
                dto.setEventLocationName(event.getLocationName());
                dto.setEventLat(event.getLat());
                dto.setEventLng(event.getLng());
            }
        }
        
        // Mapear el consumption
        if (ticket.getTicketConsumption() != null) {
            TicketConsumptionDTO consumptionDTO = modelMapper.map(ticket.getTicketConsumption(), TicketConsumptionDTO.class);
            
            // Force mapping of details because ModelMapper might fail with different field names (consumptionDetails vs ticketDetails)
            List<TicketConsumptionDetail> entityDetails = ticket.getTicketConsumption().getConsumptionDetails();
            if (entityDetails != null && !entityDetails.isEmpty()) {
                List<TicketConsumptionDetailDTO> detailDTOs = entityDetails.stream()
                    .map(detail -> {
                        TicketConsumptionDetailDTO detailDTO = modelMapper.map(detail, TicketConsumptionDetailDTO.class);
                        if (detail.getConsumption() != null) {
                            detailDTO.setConsumptionName(detail.getConsumption().getName());
                        }
                        return detailDTO;
                    })
                    .toList();
                consumptionDTO.setTicketDetails(detailDTOs);
            }
            
            dto.setTicketConsumption(consumptionDTO);
        }
        
        log.info("DTO created for ticket {}. QR Code in DTO: {}", ticket.getId(), dto.getQrCode() != null && !dto.getQrCode().isEmpty());
        return dto;
    }

    @Override
    @Transactional
    public List<TicketDTO> findByUserIdAndActive(Long userId, boolean active) {
        List<Ticket> tickets = active ? 
                ticketRepository.findByUserIdAndActiveTrue(userId) : 
                ticketRepository.findByUserId(userId).stream()
                        .filter(ticket -> !ticket.isActive())
                        .toList();
        return tickets.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<TicketDTO> findByUserIdAndRedeemed(Long userId, boolean redeemed) {
        List<Ticket> tickets = redeemed ? 
                ticketRepository.findByUserIdAndRedeemedTrue(userId) : 
                ticketRepository.findByUserIdAndRedeemedFalse(userId);
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, TicketDTO.class))
                .toList();
    }

    @Override
    public List<TicketDTO> findByEventId(Long eventId) {
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        return tickets.stream()
                .map(ticket -> modelMapper.map(ticket, TicketDTO.class))
                .toList();
    }

    @Override
    public Long countTicketsByEventId(Long eventId) {
        return ticketRepository.countTicketsByEventId(eventId);
    }

    @Override
    public Long countRedeemedTicketsByEventId(Long eventId) {
        return ticketRepository.countRedeemedTicketsByEventId(eventId);
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TicketWithConsumptionsResponse createTicketWithConsumptions(CreateTicketWithConsumptionsRequest request) {
        // 1. Buscar el evento y validar que tenga passes disponibles
    Event event = eventRepository.findByIdWithLock(request.getEventId())
        .orElseThrow(() -> new com.packed_go.event_service.exceptions.ResourceNotFoundException("Event with id " + request.getEventId() + " not found"));

    if (!event.hasAvailablePasses()) {
        throw new com.packed_go.event_service.exceptions.NoPassesAvailableException(request.getEventId());
    }

        // 2. Buscar Pass disponible con bloqueo pesimista
        List<Pass> availablePasses = passRepository.findAvailablePassesByEventIdWithLock(request.getEventId());
        if (availablePasses.isEmpty()) {
            throw new com.packed_go.event_service.exceptions.NoPassesAvailableException(request.getEventId());
        }
        Pass pass = availablePasses.get(0);

        // 3. Crear TicketConsumption
        TicketConsumption ticketConsumption = new TicketConsumption();
        ticketConsumption.setActive(true);
        ticketConsumption.setRedeem(false);
        ticketConsumption = ticketConsumptionRepository.save(ticketConsumption);

        // 4. Crear Ticket (usar purchasedAt del request si está disponible)
        Ticket ticket = new Ticket(request.getUserId(), pass, ticketConsumption, request.getPurchasedAt());
        ticket = ticketRepository.save(ticket);

        // 5. Generar QR Code (texto y imagen)
        String qrData = generateQRData(ticket, event);
        String qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrData);
        ticket.setQrText(qrData);        // Guardar formato texto para validación manual
        ticket.setQrCode(qrCodeBase64);  // Guardar imagen base64 para escaneo
        ticket = ticketRepository.save(ticket);

        // 6. Crear TicketConsumptionDetails (si existen consumptions)
        List<TicketConsumptionDetailDTO> detailDTOs = new ArrayList<>();
        if (request.getConsumptions() != null && !request.getConsumptions().isEmpty()) {
            for (ConsumptionItemDTO item : request.getConsumptions()) {
            Consumption consumption = consumptionRepository.findById(item.getConsumptionId())
                .orElseThrow(() -> new com.packed_go.event_service.exceptions.ResourceNotFoundException("Consumption with id " + item.getConsumptionId() + " not found"));

                TicketConsumptionDetail detail = new TicketConsumptionDetail();
                detail.setConsumption(consumption);
                detail.setTicketConsumption(ticketConsumption);
                detail.setQuantity(item.getQuantity());
                detail.setPriceAtPurchase(item.getPriceAtPurchase());
                detail.setActive(true);
                detail.setRedeem(false);
                detail = ticketConsumptionDetailRepository.save(detail);

                // Mapear a DTO
                TicketConsumptionDetailDTO detailDTO = modelMapper.map(detail, TicketConsumptionDetailDTO.class);
                detailDTO.setConsumptionName(consumption.getName());
                detailDTOs.add(detailDTO);
            }
        }

        // 7. Marcar el Pass como vendido
        pass.setSold(true);
        pass.setAvailable(false);
        pass.setSoldToUserId(request.getUserId());
        pass.setSoldAt(LocalDateTime.now());
        passRepository.save(pass);

        // 8. Actualizar Event
        event.markPassAsSold();
        eventRepository.save(event);

        // 9. Construir respuesta
        TicketConsumptionDTO ticketConsumptionDTO = new TicketConsumptionDTO();
        ticketConsumptionDTO.setId(ticketConsumption.getId());
        ticketConsumptionDTO.setRedeem(ticketConsumption.isRedeem());
        ticketConsumptionDTO.setTicketDetails(detailDTOs);

        return TicketWithConsumptionsResponse.builder()
                .ticketId(ticket.getId())
                .userId(ticket.getUserId())
                .passCode(pass.getCode())
                .passId(pass.getId())
                .qrCode(ticket.getQrCode())
                .eventId(event.getId())
                .eventName(event.getName())
                .eventDate(event.getEventDate())
                .eventLocation(event.getLat() + "," + event.getLng())
                .active(ticket.isActive())
                .redeemed(ticket.isRedeemed())
                .createdAt(ticket.getCreatedAt())
                .purchasedAt(ticket.getPurchasedAt())
                .redeemedAt(ticket.getRedeemedAt())
                .ticketConsumption(ticketConsumptionDTO)
                .build();
    }

    /**
     * Generates QR code data for a ticket
     * Format: PACKEDGO|T:ticketId|TC:ticketConsumptionId|E:eventId|U:userId|TS:timestamp
     */
    private String generateQRData(Ticket ticket, Event event) {
        return String.format("PACKEDGO|T:%d|TC:%d|E:%d|U:%d|TS:%d",
                ticket.getId(),
                ticket.getTicketConsumption().getId(),
                event.getId(),
                ticket.getUserId(),
                System.currentTimeMillis()
        );
    }

    @Override
    @Transactional
    public boolean isTicketRedeemed(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket with id " + ticketId + " not found"));
        return ticket.isRedeemed();
    }

    @Recover
    public TicketDTO recoverCreateTicket(Exception ex, CreateTicketDTO createTicketDTO) {
        throw new RuntimeException("No se pudo crear el ticket después de varios intentos debido a concurrencia: " + ex.getMessage(), ex);
    }

    @Recover
    public TicketDTO recoverPurchaseTicket(Exception ex, Long userId, String passCode, Long ticketConsumptionId) {
        throw new RuntimeException("No se pudo comprar el ticket después de varios intentos debido a concurrencia: " + ex.getMessage(), ex);
    }

    @Recover
    public TicketDTO recoverRedeemTicket(Exception ex, Long ticketId) {
        throw new RuntimeException("No se pudo canjear el ticket después de varios intentos debido a concurrencia: " + ex.getMessage(), ex);
    }

    @Override
    public java.util.Map<String, Long> getTicketRedemptionStatsByOrganizer(Long organizerId) {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        
        try {
            Long totalTickets = ticketRepository.countTicketsByOrganizer(organizerId);
            Long redeemedTickets = ticketRepository.countRedeemedTicketsByOrganizer(organizerId);
            
            stats.put("totalSold", totalTickets);
            stats.put("totalRedeemed", redeemedTickets);
        } catch (Exception e) {
            stats.put("totalSold", 0L);
            stats.put("totalRedeemed", 0L);
        }
        
        return stats;
    }
}
