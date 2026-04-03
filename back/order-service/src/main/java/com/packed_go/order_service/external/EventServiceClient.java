package com.packed_go.order_service.external;

import com.packed_go.order_service.dto.external.ConsumptionDTO;
import com.packed_go.order_service.dto.external.CreateTicketWithConsumptionsRequest;
import com.packed_go.order_service.dto.external.EventDTO;
import com.packed_go.order_service.dto.external.TicketWithConsumptionsResponse;
import com.packed_go.order_service.exception.EventNotFoundException;
import com.packed_go.order_service.exception.ServiceCommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class EventServiceClient {

    private final WebClient webClient;
    
    public EventServiceClient(@Qualifier("eventServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Obtiene los detalles de un evento por su ID
     */
    public EventDTO getEventById(Long eventId) {
        try {
            log.info("Fetching event details for eventId: {}", eventId);
            
            EventDTO event = webClient.get()
                    .uri("/event-service/event/{id}", eventId)
                    .retrieve()
                    .bodyToMono(EventDTO.class)
                    .timeout(Duration.ofSeconds(10))
                    .block(); // Bloquea y convierte a síncrono
            
            if (event == null) {
                throw new EventNotFoundException(eventId);
            }
            
            log.info("Successfully fetched event: {} (ID: {})", event.getName(), eventId);
            return event;
            
        } catch (WebClientResponseException.NotFound e) {
            log.error("Event not found with id: {}", eventId);
            throw new EventNotFoundException(eventId);
        } catch (Exception e) {
            log.error("Error communicating with EVENT-SERVICE for eventId: {}", eventId, e);
            throw new ServiceCommunicationException("Failed to fetch event from EVENT-SERVICE", e);
        }
    }

    /**
     * Obtiene las consumiciones disponibles para un evento
     */
    public List<ConsumptionDTO> getEventConsumptions(Long eventId) {
        try {
            log.info("Fetching consumptions for eventId: {}", eventId);
            
            List<ConsumptionDTO> consumptions = webClient.get()
                    .uri("/event-service/event/{eventId}/consumptions", eventId)
                    .retrieve()
                    .bodyToFlux(ConsumptionDTO.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            log.info("Successfully fetched {} consumptions for event: {}", 
                    consumptions != null ? consumptions.size() : 0, eventId);
            return consumptions;
            
        } catch (WebClientResponseException.NotFound e) {
            log.error("Consumptions not found for eventId: {}", eventId);
            throw new EventNotFoundException(eventId);
        } catch (Exception e) {
            log.error("Error fetching consumptions for eventId: {}", eventId, e);
            throw new ServiceCommunicationException("Failed to fetch consumptions from EVENT-SERVICE", e);
        }
    }

    /**
     * Verifica si hay passes disponibles para un evento
     */
    public boolean checkPassAvailability(Long eventId) {
        try {
            log.info("Checking pass availability for eventId: {}", eventId);
            
            EventDTO event = getEventById(eventId);
            boolean available = event.getAvailablePasses() != null && event.getAvailablePasses() > 0;
            
            log.info("Pass availability for event {}: {} (available: {})", 
                    eventId, available, event.getAvailablePasses());
            return available;
            
        } catch (Exception e) {
            log.error("Error checking pass availability for eventId: {}", eventId, e);
            throw new ServiceCommunicationException("Failed to check pass availability", e);
        }
    }

    /**
     * Obtiene la cantidad de passes disponibles para un evento
     */
    public Integer getAvailablePassesCount(Long eventId) {
        try {
            EventDTO event = getEventById(eventId);
            return event.getAvailablePasses() != null ? event.getAvailablePasses() : 0;
        } catch (Exception e) {
            log.error("Error getting available passes count for eventId: {}", eventId, e);
            return 0;
        }
    }

    /**
     * Versión reactiva de getEventById (para uso futuro)
     */
    public Mono<EventDTO> getEventByIdAsync(Long eventId) {
        log.info("Fetching event details asynchronously for eventId: {}", eventId);
        
        return webClient.get()
                .uri("/event-service/event/{id}", eventId)
                .retrieve()
                .bodyToMono(EventDTO.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(event -> log.info("Successfully fetched event: {}", event.getName()))
                .doOnError(error -> log.error("Error fetching event: {}", eventId, error));
    }

    /**
     * Crea un ticket con consumiciones para un usuario en un evento
     * Este método es llamado cuando una orden es pagada exitosamente
     */
    public TicketWithConsumptionsResponse createTicketWithConsumptions(CreateTicketWithConsumptionsRequest request) {
        try {
            log.info("Creating ticket with consumptions for userId: {}, eventId: {}", 
                    request.getUserId(), request.getEventId());
            
            TicketWithConsumptionsResponse response = webClient.post()
                    .uri("/event-service/tickets/create-with-consumptions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TicketWithConsumptionsResponse.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            
            log.info("Successfully created ticket with ID: {} for userId: {}", 
                    response.getTicketId(), request.getUserId());
            return response;
            
        } catch (WebClientResponseException e) {
            log.error("Error creating ticket with consumptions. Status: {}, Body: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServiceCommunicationException("Failed to create ticket in EVENT-SERVICE: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error creating ticket with consumptions for userId: {}, eventId: {}", 
                    request.getUserId(), request.getEventId(), e);
            throw new ServiceCommunicationException("Failed to create ticket with consumptions", e);
        }
    }
}

