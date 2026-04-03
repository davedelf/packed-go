package com.packed_go.auth_service.repositories;

import com.packed_go.auth_service.entities.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findByLoginIdentifierAndAttemptedAtAfter(
            String loginIdentifier, LocalDateTime after);
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.loginIdentifier = :identifier " +
           "AND l.success = false AND l.attemptedAt > :after")
    Long countFailedAttempts(@Param("identifier") String identifier, @Param("after") LocalDateTime after);
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.ipAddress = :ipAddress " +
           "AND l.success = false AND l.attemptedAt > :after")
    Long countFailedAttemptsByIp(@Param("ipAddress") String ipAddress, @Param("after") LocalDateTime after);
    
    List<LoginAttempt> findByLoginIdentifierOrderByAttemptedAtDesc(String loginIdentifier);
    
    @Query("DELETE FROM LoginAttempt l WHERE l.attemptedAt < :before")
    void deleteOldAttempts(@Param("before") LocalDateTime before);
}
