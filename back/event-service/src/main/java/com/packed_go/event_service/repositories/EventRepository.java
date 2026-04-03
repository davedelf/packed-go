package com.packed_go.event_service.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.packed_go.event_service.entities.Event;

import jakarta.persistence.LockModeType;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findById(Long id);

    boolean existsById(Long id);

    Optional<List<Event>> findAllByStatus(String status);

    Optional<List<Event>> findAllByEventDate(LocalDateTime eventDate);

    Optional<List<Event>> findAllByEventDateAndStatus(LocalDateTime eventDate, String status);

    // Optional<List<Event>> findByLocation(Point location);

    // Métodos para gestión de Pass
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT e FROM Event e WHERE e.availablePasses > 0")
    List<Event> findEventsWithAvailablePasses();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.id = :eventId AND e.availablePasses > 0")
    Long hasAvailablePasses(@Param("eventId") Long eventId);

    // ========== QUERIES MULTI-TENANT (NUEVAS) ==========
    /**
     * 🔒 Encuentra un evento por ID y createdBy (validación de ownership)
     */
    Optional<Event> findByIdAndCreatedBy(Long id, Long createdBy);
    
    /**
     * 🔒 Encuentra todos los eventos de un usuario específico
     */
    List<Event> findByCreatedBy(Long createdBy);
    
    /**
     * 🔒 Encuentra eventos activos de un usuario
     */
    List<Event> findByCreatedByAndStatus(Long createdBy, String status);
    
    // ========== QUERIES PARA ESTADÍSTICAS ==========
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdBy = :createdBy")
    Long countByCreatedBy(@Param("createdBy") Long createdBy);
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdBy = :createdBy AND e.active = true")
    Long countActiveByCreatedBy(@Param("createdBy") Long createdBy);
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdBy = :createdBy AND e.eventDate > :now")
    Long countUpcomingByCreatedBy(@Param("createdBy") Long createdBy, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdBy = :createdBy AND e.eventDate < :now")
    Long countPastByCreatedBy(@Param("createdBy") Long createdBy, @Param("now") LocalDateTime now);
    
    @Query("SELECT COALESCE(SUM(e.maxCapacity), 0) FROM Event e WHERE e.createdBy = :createdBy")
    Long sumMaxCapacityByCreatedBy(@Param("createdBy") Long createdBy);
    
    @Query("SELECT COALESCE(SUM(e.availablePasses), 0) FROM Event e WHERE e.createdBy = :createdBy")
    Long sumAvailablePassesByCreatedBy(@Param("createdBy") Long createdBy);
    
    @Query("SELECT COALESCE(SUM(e.soldPasses), 0) FROM Event e WHERE e.createdBy = :createdBy")
    Long sumSoldPassesByCreatedBy(@Param("createdBy") Long createdBy);

}
