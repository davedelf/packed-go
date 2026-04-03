package com.packed_go.event_service.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.packed_go.event_service.entities.TicketConsumption;

import jakarta.persistence.LockModeType;

@Repository
public interface TicketConsumptionRepository extends JpaRepository<TicketConsumption,Long> {
    
    // Buscar un ticket específico con bloqueo pesimista para evitar concurrencia
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tc FROM TicketConsumption tc WHERE tc.id = :id")
    Optional<TicketConsumption> findByIdWithLock(@Param("id") Long id);

    // Contar todos los TicketConsumptions (consumiciones vendidas) por organizador
    @Query(value = "SELECT COUNT(DISTINCT ct.id) FROM consumption_tickets ct " +
                   "INNER JOIN tickets t ON t.ticket_consumption_id = ct.id " +
                   "INNER JOIN passes p ON t.pass_id = p.id " +
                   "INNER JOIN events e ON p.event_id = e.id " +
                   "WHERE e.created_by = :organizerId AND ct.active = true", nativeQuery = true)
    Long countTicketsByOrganizer(@Param("organizerId") Long organizerId);

    // Contar TicketConsumptions canjeados por organizador
    @Query(value = "SELECT COUNT(DISTINCT ct.id) FROM consumption_tickets ct " +
                   "INNER JOIN tickets t ON t.ticket_consumption_id = ct.id " +
                   "INNER JOIN passes p ON t.pass_id = p.id " +
                   "INNER JOIN events e ON p.event_id = e.id " +
                   "WHERE e.created_by = :organizerId AND ct.active = true AND ct.redeem = true", nativeQuery = true)
    Long countRedeemedTicketsByOrganizer(@Param("organizerId") Long organizerId);
}
