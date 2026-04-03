package com.packed_go.auth_service.repositories;

import com.packed_go.auth_service.entities.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionToken(String sessionToken);
    
    Optional<UserSession> findByRefreshToken(String refreshToken);
    
    List<UserSession> findByAuthUserIdAndIsActive(Long authUserId, Boolean isActive);
    
    List<UserSession> findByAuthUserId(Long authUserId);
    
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.isActive = true")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.authUserId = :userId")
    void deactivateAllUserSessions(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.sessionToken = :sessionToken")
    void deactivateSession(@Param("sessionToken") String sessionToken);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivity = :lastActivity WHERE s.sessionToken = :sessionToken")
    void updateLastActivity(@Param("sessionToken") String sessionToken, @Param("lastActivity") LocalDateTime lastActivity);
    
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
}
