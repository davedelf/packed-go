package com.packed_go.auth_service.repositories;

import com.packed_go.auth_service.entities.PasswordRecoveryToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {

    Optional<PasswordRecoveryToken> findByToken(String token);
    
    Optional<PasswordRecoveryToken> findByTokenAndIsUsedFalse(String token);
    
    Optional<PasswordRecoveryToken> findByAuthUserIdAndIsUsedFalseAndExpiresAtAfter(
            Long authUserId, LocalDateTime now);
    
    @Modifying
    @Query("UPDATE PasswordRecoveryToken t SET t.isUsed = true, t.usedAt = :usedAt WHERE t.token = :token")
    void markAsUsed(@Param("token") String token, @Param("usedAt") LocalDateTime usedAt);
    
    @Modifying
    @Query("DELETE FROM PasswordRecoveryToken t WHERE t.expiresAt < :now OR t.isUsed = true")
    void deleteExpiredOrUsedTokens(@Param("now") LocalDateTime now);
}
