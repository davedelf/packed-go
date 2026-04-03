package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.consumption.SimpleConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionWithDetailsDTO;
import com.packed_go.event_service.dtos.ticketConsumption.CreateTicketConsumptionWithSimpleDetailsDTO;
import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.CreateTicketConsumptionDetailDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.CreateTicketConsumptionDetailSimpleDTO;
import com.packed_go.event_service.dtos.ticketConsumptionDetail.TicketConsumptionDetailDTO;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.entities.TicketConsumption;
import com.packed_go.event_service.entities.TicketConsumptionDetail;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.repositories.TicketConsumptionDetailRepository;
import com.packed_go.event_service.repositories.TicketConsumptionRepository;
import com.packed_go.event_service.services.TicketConsumptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TicketConsumptionServiceImpl implements TicketConsumptionService {
    private final ModelMapper modelMapper;
    private final TicketConsumptionRepository ticketConsumptionRepository;
    private final ConsumptionRepository consumptionRepository;
    private final TicketConsumptionDetailRepository ticketConsumptionDetailRepository;


    @Override
    @Transactional
    public TicketConsumptionDTO create(TicketConsumptionDTO ticketConsumptionDTO) {
        TicketConsumption ticket = new TicketConsumption();
        ticket.setActive(true);
        ticket = ticketConsumptionRepository.save(ticket);

        List<TicketConsumptionDetail> details = new ArrayList<>();
        for (TicketConsumptionDetailDTO detailDto : ticketConsumptionDTO.getTicketDetails()) {
            //Validar existencia y que esté activo
            Consumption consumption = consumptionRepository.findById(detailDto.getConsumptionId())
                    .orElseThrow(() -> new RuntimeException("Consumption with id " + detailDto.getConsumptionId() + " not found"));
            if (!consumption.isActive()) {
                throw new RuntimeException("Consumption with id " + detailDto.getConsumptionId() + " is not active");
            }

            //Mapeamos DTOS a Entidades
            TicketConsumptionDetail detail = modelMapper.map(detailDto, TicketConsumptionDetail.class);
            detail.setTicketConsumption(ticket);
            detail.setConsumption(consumption);

            //Guardamos el preico historico
            detail.setPriceAtPurchase(consumption.getPrice());

            details.add(detail);
        }

        //Guardamos todos los detalles en batch
        ticketConsumptionDetailRepository.saveAll(details);

        //Asociar detalles al ticket

        ticket.setConsumptionDetails(details);


        return modelMapper.map(ticket, TicketConsumptionDTO.class);

    }

    @Override
    @Transactional
    public TicketConsumptionDTO createFromConsumptions(CreateTicketConsumptionDTO createTicketConsumptionDTO) {
        // Crear el ticket
        TicketConsumption ticket = new TicketConsumption();
        ticket.setActive(true);
        ticket = ticketConsumptionRepository.save(ticket);

        List<TicketConsumptionDetail> details = new ArrayList<>();
        
        for (SimpleConsumptionDTO simpleConsumptionDTO : createTicketConsumptionDTO.getConsumptions()) {
            // Validar que el consumption existe y está activo
            Consumption consumption = consumptionRepository.findById(simpleConsumptionDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Consumption with id " + simpleConsumptionDTO.getId() + " not found"));
            
            if (!consumption.isActive()) {
                throw new RuntimeException("Consumption with id " + simpleConsumptionDTO.getId() + " is not active");
            }

            // Crear el detalle del ticket
            TicketConsumptionDetail detail = new TicketConsumptionDetail();
            detail.setTicketConsumption(ticket);
            detail.setConsumption(consumption);
            detail.setQuantity(1); // Cantidad por defecto
            detail.setPriceAtPurchase(consumption.getPrice());
            detail.setActive(true);

            details.add(detail);
        }

        // Guardar todos los detalles en batch
        ticketConsumptionDetailRepository.saveAll(details);

        // Asociar detalles al ticket
        ticket.setConsumptionDetails(details);

        return modelMapper.map(ticket, TicketConsumptionDTO.class);
    }

    @Override
    @Transactional
    public TicketConsumptionDTO createWithDetails(CreateTicketConsumptionWithDetailsDTO createTicketConsumptionWithDetailsDTO) {
        // Crear el ticket maestro
        TicketConsumption ticket = new TicketConsumption();
        ticket.setActive(true);
        ticket = ticketConsumptionRepository.save(ticket);

        List<TicketConsumptionDetail> details = new ArrayList<>();
        
        for (CreateTicketConsumptionDetailDTO detailDTO : createTicketConsumptionWithDetailsDTO.getDetails()) {
            // Validar que el consumption existe y está activo
            Consumption consumption = consumptionRepository.findById(detailDTO.getConsumption().getId())
                    .orElseThrow(() -> new RuntimeException("Consumption with id " + detailDTO.getConsumption().getId() + " not found"));
            
            if (!consumption.isActive()) {
                throw new RuntimeException("Consumption with id " + detailDTO.getConsumption().getId() + " is not active");
            }

            // Validar que la cantidad sea válida
            if (detailDTO.getQuantity() == null || detailDTO.getQuantity() <= 0) {
                throw new RuntimeException("Quantity must be greater than 0 for consumption " + consumption.getName());
            }

            // Crear el detalle del ticket
            TicketConsumptionDetail detail = new TicketConsumptionDetail();
            detail.setTicketConsumption(ticket);
            detail.setConsumption(consumption);
            detail.setQuantity(detailDTO.getQuantity());
            detail.setPriceAtPurchase(consumption.getPrice());
            detail.setActive(true);

            details.add(detail);
        }

        // Guardar todos los detalles en batch (patrón maestro-detalle)
        ticketConsumptionDetailRepository.saveAll(details);

        // Asociar detalles al ticket maestro
        ticket.setConsumptionDetails(details);

        return modelMapper.map(ticket, TicketConsumptionDTO.class);
    }

    @Override
    @Transactional
    public TicketConsumptionDTO createWithSimpleDetails(CreateTicketConsumptionWithSimpleDetailsDTO createTicketConsumptionWithSimpleDetailsDTO) {
        // Crear el ticket maestro
        TicketConsumption ticket = new TicketConsumption();
        ticket.setActive(true);
        ticket = ticketConsumptionRepository.save(ticket);

        List<TicketConsumptionDetail> details = new ArrayList<>();
        
        for (CreateTicketConsumptionDetailSimpleDTO detailDTO : createTicketConsumptionWithSimpleDetailsDTO.getDetails()) {
            // Validar que el consumption existe y está activo
            Consumption consumption = consumptionRepository.findById(detailDTO.getConsumptionId())
                    .orElseThrow(() -> new RuntimeException("Consumption with id " + detailDTO.getConsumptionId() + " not found"));
            
            if (!consumption.isActive()) {
                throw new RuntimeException("Consumption with id " + detailDTO.getConsumptionId() + " is not active");
            }

            // Validar que la cantidad sea válida
            if (detailDTO.getQuantity() == null || detailDTO.getQuantity() <= 0) {
                throw new RuntimeException("Quantity must be greater than 0 for consumption " + consumption.getName());
            }

            // Crear el detalle del ticket
            TicketConsumptionDetail detail = new TicketConsumptionDetail();
            detail.setTicketConsumption(ticket);
            detail.setConsumption(consumption);
            detail.setQuantity(detailDTO.getQuantity());
            detail.setPriceAtPurchase(consumption.getPrice());
            detail.setActive(true);

            details.add(detail);
        }

        // Guardar todos los detalles en batch (patrón maestro-detalle)
        ticketConsumptionDetailRepository.saveAll(details);

        // Asociar detalles al ticket maestro
        ticket.setConsumptionDetails(details);

        return modelMapper.map(ticket, TicketConsumptionDTO.class);
    }

    @Override
    public TicketConsumption update(Long id, TicketConsumptionDTO ticketConsumptionDTO) {
        return null;
    }

    @Override
    public TicketConsumption deleteLogical(Long id) {
        return null;
    }

    @Override
    public boolean redeemTicketDetails() {
        return false;
    }

    @Override
    public List<TicketConsumptionDetailDTO> getTicketConsumptionDetails(Long id) {
        return List.of();
    }

    @Override
    public TicketConsumptionDTO findById(Long id) {
        TicketConsumption ticket = ticketConsumptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TicketConsumption with id " + id + " not found"));
        return modelMapper.map(ticket, TicketConsumptionDTO.class);
    }

    @Override
    public java.util.Map<String, Long> getRedemptionStatsByOrganizer(Long organizerId) {
        // Usar consulta nativa para obtener estadísticas de redención por organizador
        // La relación es: tickets -> passes -> events (created_by = organizerId)
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        
        try {
            Long totalTickets = ticketConsumptionRepository.countTicketsByOrganizer(organizerId);
            Long redeemedTickets = ticketConsumptionRepository.countRedeemedTicketsByOrganizer(organizerId);
            
            stats.put("totalSold", totalTickets);
            stats.put("totalRedeemed", redeemedTickets);
        } catch (Exception e) {
            stats.put("totalSold", 0L);
            stats.put("totalRedeemed", 0L);
        }
        
        return stats;
    }
}
