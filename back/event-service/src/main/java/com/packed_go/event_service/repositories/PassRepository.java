package com.packed_go.event_service.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.packed_go.event_service.entities.Pass;

import jakarta.persistence.LockModeType;

@Repository
public interface PassRepository extends JpaRepository<Pass, Long> {

    // Buscar pass por código
    Optional<Pass> findByCode(String code);

    // Buscar pass por código con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pass p WHERE p.code = :code")
    Optional<Pass> findByCodeWithLock(@Param("code") String code);

    // Buscar todos los pass de un evento
    List<Pass> findByEvent_Id(Long eventId);

    // Buscar pass disponibles de un evento
    List<Pass> findByEvent_IdAndAvailableTrue(Long eventId);

    // Buscar pass vendidos de un evento
    List<Pass> findByEvent_IdAndSoldTrue(Long eventId);

    // Buscar pass por usuario comprador
    List<Pass> findBySoldToUserId(Long userId);

    // Contar pass disponibles de un evento
    @Query("SELECT COUNT(p) FROM Pass p WHERE p.event.id = :eventId AND p.available = true")
    Long countAvailablePassesByEventId(@Param("eventId") Long eventId);

    // Contar pass vendidos de un evento
    @Query("SELECT COUNT(p) FROM Pass p WHERE p.event.id = :eventId AND p.sold = true")
    Long countSoldPassesByEventId(@Param("eventId") Long eventId);

    // Buscar un pass disponible de un evento específico con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pass p WHERE p.event.id = :eventId AND p.available = true ORDER BY p.id ASC")
    List<Pass> findAvailablePassesByEventIdWithLock(@Param("eventId") Long eventId);

    // Buscar pass por ID con bloqueo pesimista
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pass p WHERE p.id = :id")
    Optional<Pass> findByIdWithLock(@Param("id") Long id);

    // Buscar pass por los últimos 8 caracteres del código
    @Query("SELECT p FROM Pass p WHERE p.code LIKE CONCAT('%', :codeSuffix)")
    List<Pass> findByCodeSuffix(@Param("codeSuffix") String codeSuffix);
}
