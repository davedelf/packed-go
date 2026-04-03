package com.packed_go.event_service.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.packed_go.event_service.entities.Ticket;

import jakarta.persistence.LockModeType;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Buscar tickets por usuario con pass eager loaded
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.pass WHERE t.userId = :userId")
    List<Ticket> findByUserId(@Param("userId") Long userId);

    // Buscar tickets por usuario y activos
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.pass WHERE t.userId = :userId AND t.active = true")
    List<Ticket> findByUserIdAndActiveTrue(@Param("userId") Long userId);

    // Buscar ticket por pass
    Optional<Ticket> findByPass_Id(Long passId);

    // Buscar ticket por pass con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.pass.id = :passId")
    Optional<Ticket> findByPass_IdWithLock(@Param("passId") Long passId);

    // Buscar tickets canjeados por usuario
    List<Ticket> findByUserIdAndRedeemedTrue(Long userId);

    // Buscar tickets no canjeados por usuario
    List<Ticket> findByUserIdAndRedeemedFalse(Long userId);

    // Buscar tickets por evento (a través del pass)
    @Query("SELECT t FROM Ticket t WHERE t.pass.event.id = :eventId")
    List<Ticket> findByEventId(@Param("eventId") Long eventId);

    // Contar tickets vendidos por evento
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.pass.event.id = :eventId")
    Long countTicketsByEventId(@Param("eventId") Long eventId);

    // Contar tickets canjeados por evento
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.pass.event.id = :eventId AND t.redeemed = true")
    Long countRedeemedTicketsByEventId(@Param("eventId") Long eventId);

    // Buscar ticket por código de pass
    @Query("SELECT t FROM Ticket t WHERE t.pass.code = :passCode")
    Optional<Ticket> findByPassCode(@Param("passCode") String passCode);

    // Buscar ticket por código de pass con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.pass.code = :passCode")
    Optional<Ticket> findByPassCodeWithLock(@Param("passCode") String passCode);

    // Buscar ticket por ID con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdWithLock(@Param("id") Long id);

    // Buscar ticket por ID con Pass eager loaded
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.pass p LEFT JOIN FETCH p.event WHERE t.id = :id")
    Optional<Ticket> findByIdWithPass(@Param("id") Long id);

    // Buscar ticket por TicketConsumption
    @Query("SELECT t FROM Ticket t WHERE t.ticketConsumption = :ticketConsumption")
    Optional<Ticket> findByTicketConsumption(@Param("ticketConsumption") com.packed_go.event_service.entities.TicketConsumption ticketConsumption);

    // Buscar ticket por ID de TicketConsumption
    @Query("SELECT t FROM Ticket t WHERE t.ticketConsumption.id = :ticketConsumptionId")
    Optional<Ticket> findByTicketConsumption_Id(@Param("ticketConsumptionId") Long ticketConsumptionId);

    // Contar todos los tickets (entradas) por organizador
    @Query(value = "SELECT COUNT(t.id) FROM tickets t " +
                   "INNER JOIN passes p ON t.pass_id = p.id " +
                   "INNER JOIN events e ON p.event_id = e.id " +
                   "WHERE e.created_by = :organizerId AND t.active = true", nativeQuery = true)
    Long countTicketsByOrganizer(@Param("organizerId") Long organizerId);

    // Contar tickets (entradas) canjeados por organizador
    @Query(value = "SELECT COUNT(t.id) FROM tickets t " +
                   "INNER JOIN passes p ON t.pass_id = p.id " +
                   "INNER JOIN events e ON p.event_id = e.id " +
                   "WHERE e.created_by = :organizerId AND t.active = true AND t.redeemed = true", nativeQuery = true)
    Long countRedeemedTicketsByOrganizer(@Param("organizerId") Long organizerId);
}
