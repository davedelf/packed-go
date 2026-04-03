package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.TicketConsumptionDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketConsumptionDetailRepository extends JpaRepository<TicketConsumptionDetail, Long> {

    // Buscar por el id del ticket padre
    List<TicketConsumptionDetail> findByTicketConsumption_Id(Long ticketId);

    // Buscar por el id del consumo
    List<TicketConsumptionDetail> findByConsumption_Id(Long consumptionId);

    // Buscar por el nombre del consumo
    List<TicketConsumptionDetail> findByConsumption_Name(String name);

    // Buscar un detalle específico con bloqueo pesimista para evitar concurrencia
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tcd FROM TicketConsumptionDetail tcd WHERE tcd.id = :id")
    Optional<TicketConsumptionDetail> findByIdWithLock(@Param("id") Long id);

    // Buscar todos los detalles de un ticket con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT tcd FROM TicketConsumptionDetail tcd WHERE tcd.ticketConsumption.id = :ticketId")
    List<TicketConsumptionDetail> findByTicketConsumption_IdWithLock(@Param("ticketId") Long ticketId);
}
