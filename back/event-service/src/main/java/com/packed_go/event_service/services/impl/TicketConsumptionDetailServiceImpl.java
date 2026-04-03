package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.ticketConsumptionDetail.RedeemTicketDetailDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.entities.TicketConsumption;
import com.packed_go.event_service.entities.TicketConsumptionDetail;
import com.packed_go.event_service.entities.Ticket;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.repositories.TicketConsumptionDetailRepository;
import com.packed_go.event_service.repositories.TicketConsumptionRepository;
import com.packed_go.event_service.repositories.TicketRepository;
import com.packed_go.event_service.services.TicketConsumptionDetailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketConsumptionDetailServiceImpl implements TicketConsumptionDetailService {
    private final ConsumptionRepository consumptionRepository;
    private final TicketConsumptionDetailRepository ticketConsumptionDetailRepository;
    private final TicketConsumptionRepository ticketConsumptionRepository;
    private final TicketRepository ticketRepository;
    private final ModelMapper modelMapper;

    @Override
    public TicketConsumptionDetailDTO create(TicketConsumptionDetailDTO ticketConsumptionDetailDTO) {
        Consumption consumption = consumptionRepository.findById(ticketConsumptionDetailDTO.getConsumptionId()).orElseThrow(() -> new RuntimeException("Consumption with id " + ticketConsumptionDetailDTO.getConsumptionId() + " not found"));
        TicketConsumptionDetail ticketConsumptionDetail = modelMapper.map(ticketConsumptionDetailDTO, TicketConsumptionDetail.class);
        ticketConsumptionDetail.setConsumption(consumption);
        TicketConsumptionDetail saved = ticketConsumptionDetailRepository.save(ticketConsumptionDetail);
        return modelMapper.map(saved, TicketConsumptionDetailDTO.class);

    }

    @Override
    public TicketConsumptionDetailDTO update(Long id, TicketConsumptionDetailDTO dto) {

        TicketConsumptionDetail existingDetail = ticketConsumptionDetailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TicketConsumptionDetail with id " + id + " not found"));

        Consumption consumption = consumptionRepository.findById(dto.getConsumptionId())
                .orElseThrow(() -> new RuntimeException("Consumption with id " + dto.getConsumptionId() + " not found"));

        if (!consumption.isActive()) {
            throw new RuntimeException("Consumption with id " + dto.getConsumptionId() + " is not active");
        }

        // Mapear todos los campos del DTO al detalle existente usando ModelMapper
        modelMapper.map(dto, existingDetail);

        // Sobrescribir el objeto Consumption con la entidad validada
        existingDetail.setConsumption(consumption);

        // Actualizar el priceAtPurchase con el precio actual del consumption
        existingDetail.setPriceAtPurchase(consumption.getPrice());

        // Guardar y retornar
        TicketConsumptionDetail updated = ticketConsumptionDetailRepository.save(existingDetail);
        return modelMapper.map(updated, TicketConsumptionDetailDTO.class);
    }

    @Override
    public List<TicketConsumptionDetailDTO> findAllByTicketId(Long id) {
        List<TicketConsumptionDetail> ticketDetails = ticketConsumptionDetailRepository.findByTicketConsumption_Id(id);
        return ticketDetails.stream().map(entity -> {
            TicketConsumptionDetailDTO dto = modelMapper.map(entity, TicketConsumptionDetailDTO.class);
            if (entity.getConsumption() != null) {
                dto.setConsumptionName(entity.getConsumption().getName());
            }
            return dto;
        }).toList();

    }

    @Override
    public List<TicketConsumptionDetailDTO> findAllByEntryTicketId(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket with id " + ticketId + " not found"));
        
        if (ticket.getTicketConsumption() == null) {
            return List.of();
        }
        
        return findAllByTicketId(ticket.getTicketConsumption().getId());
    }

    @Override
    public List<TicketConsumptionDetailDTO> findAllByConsumptionName(String name) {
        List<TicketConsumptionDetail> ticketDetails = ticketConsumptionDetailRepository.findByConsumption_Name(name);
        return ticketDetails.stream().map(entity -> modelMapper.map(entity, TicketConsumptionDetailDTO.class)).toList();
    }

    @Transactional
    @Override
    public TicketConsumptionDetail deleteLogical(Long id) {
        Optional<TicketConsumptionDetail> ticketDetail = ticketConsumptionDetailRepository.findById(id);
        if (ticketDetail.isPresent()) {
            TicketConsumptionDetail ticket = modelMapper.map(ticketDetail.get(), TicketConsumptionDetail.class);
            ticket.setActive(false);
            ticketConsumptionDetailRepository.save(ticket);
            return modelMapper.map(ticket, TicketConsumptionDetail.class);
        } else {
            throw new RuntimeException("TicketConsumptionDetail with id " + id + " not found");
        }
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public RedeemTicketDetailDTO redeemDetail(Long detailId) {
        RedeemTicketDetailDTO response = new RedeemTicketDetailDTO();
        response.setTicketDetailId(detailId);
        
        try {
            // Buscar el detalle del ticket con bloqueo pesimista para evitar concurrencia
            TicketConsumptionDetail detail = ticketConsumptionDetailRepository.findByIdWithLock(detailId)
                    .orElseThrow(() -> new RuntimeException("TicketConsumptionDetail with id " + detailId + " not found"));
            
            // Verificar si ya está canjeado
            if (detail.isRedeem()) {
                response.setSuccess(false);
                response.setMessage("El detalle del ticket ya está canjeado");
                return response;
            }
            
            // Verificar si el detalle está activo
            if (!detail.isActive()) {
                response.setSuccess(false);
                response.setMessage("El detalle del ticket no está activo");
                return response;
            }
            
            // Marcar el detalle como canjeado
            detail.setRedeem(true);
            ticketConsumptionDetailRepository.save(detail);
            
            // Obtener el ticket padre con bloqueo pesimista
            TicketConsumption ticket = ticketConsumptionRepository.findByIdWithLock(detail.getTicketConsumption().getId())
                    .orElseThrow(() -> new RuntimeException("TicketConsumption not found"));
            
            // Verificar si todos los detalles del ticket están canjeados
            List<TicketConsumptionDetail> allDetails = ticketConsumptionDetailRepository
                    .findByTicketConsumption_IdWithLock(ticket.getId());
            
            boolean allDetailsRedeemed = allDetails.stream()
                    .allMatch(TicketConsumptionDetail::isRedeem);
            
            response.setAllDetailsRedeemed(allDetailsRedeemed);
            
            // Si todos los detalles están canjeados, marcar el ticket como canjeado
            if (allDetailsRedeemed && !ticket.isRedeem()) {
                ticket.setRedeem(true);
                ticketConsumptionRepository.save(ticket);
                response.setTicketRedeemed(true);
                response.setMessage("Detalle canjeado exitosamente. ¡Todo el ticket ha sido canjeado!");
            } else {
                response.setTicketRedeemed(ticket.isRedeem());
                response.setMessage("Detalle canjeado exitosamente. Quedan detalles por canjear.");
            }
            
            response.setSuccess(true);
            return response;
            
        } catch (Exception e) {
            // Manejar errores de concurrencia y otros errores
            response.setSuccess(false);
            response.setMessage("Error al canjear el detalle: " + e.getMessage());
            throw new RuntimeException("Error al procesar el canje del detalle " + detailId + ": " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public TicketConsumptionDetailDTO redeemDetailPartial(Long detailId, Integer quantityToRedeem) {
        // Buscar el detalle con bloqueo pesimista
        TicketConsumptionDetail detail = ticketConsumptionDetailRepository.findByIdWithLock(detailId)
                .orElseThrow(() -> new RuntimeException("TicketConsumptionDetail with id " + detailId + " not found"));

        // Verificar que esté activo
        if (!detail.isActive()) {
            throw new RuntimeException("TicketConsumptionDetail is not active");
        }

        // Verificar que no esté completamente canjeado
        if (detail.isRedeem()) {
            throw new RuntimeException("TicketConsumptionDetail is already fully redeemed");
        }

        // Verificar que la cantidad a canjear sea válida
        if (quantityToRedeem <= 0 || quantityToRedeem > detail.getQuantity()) {
            throw new RuntimeException("Invalid quantity to redeem: " + quantityToRedeem + 
                    ". Available: " + detail.getQuantity());
        }

        // Reducir la cantidad
        detail.setQuantity(detail.getQuantity() - quantityToRedeem);

        // Si la cantidad llega a 0, marcar como canjeado
        if (detail.getQuantity() == 0) {
            detail.setRedeem(true);
        }

        detail = ticketConsumptionDetailRepository.save(detail);
        TicketConsumptionDetailDTO dto = modelMapper.map(detail, TicketConsumptionDetailDTO.class);
        if (detail.getConsumption() != null) {
            dto.setConsumptionName(detail.getConsumption().getName());
        }
        return dto;
    }

    @Override
    public TicketConsumptionDetailDTO findById(Long detailId) {
        TicketConsumptionDetail detail = ticketConsumptionDetailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("TicketConsumptionDetail with id " + detailId + " not found"));
        TicketConsumptionDetailDTO dto = modelMapper.map(detail, TicketConsumptionDetailDTO.class);
        if (detail.getConsumption() != null) {
            dto.setConsumptionName(detail.getConsumption().getName());
        }
        return dto;
    }

    @Recover
    public RedeemTicketDetailDTO recoverRedeemDetail(Exception ex, Long detailId) {
        RedeemTicketDetailDTO response = new RedeemTicketDetailDTO();
        response.setTicketDetailId(detailId);
        response.setSuccess(false);
        response.setMessage("No se pudo procesar el canje después de varios intentos debido a concurrencia. Intente nuevamente.");
        response.setAllDetailsRedeemed(false);
        response.setTicketRedeemed(false);
        return response;
    }

}
