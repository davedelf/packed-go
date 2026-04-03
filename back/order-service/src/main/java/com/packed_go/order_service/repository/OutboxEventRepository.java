package com.packed_go.order_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.packed_go.order_service.entity.OutboxEvent;
import com.packed_go.order_service.entity.OutboxEvent.EventStatus;

/**
 * Repository para el patrón Transactional Outbox.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Busca eventos pendientes ordenados por fecha de creación
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(EventStatus status);

    /**
     * Busca eventos pendientes con retry menor al límite
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND e.retryCount < 5 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsToProcess();

    /**
     * Busca eventos en DLQ para reprocesamiento manual
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'DLQ' ORDER BY e.createdAt DESC")
    List<OutboxEvent> findEventsInDeadLetterQueue();

    /**
     * Verifica si ya existe un evento para esta orden y tipo
     */
    boolean existsByAggregateIdAndEventType(String aggregateId, com.packed_go.order_service.entity.OutboxEvent.EventType eventType);
}
