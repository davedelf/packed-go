package com.packed_go.auth_service.repositories;

import com.packed_go.auth_service.entities.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByTokenAndIsVerifiedFalse(String token);
    
    Optional<EmailVerificationToken> findByAuthUserIdAndIsVerifiedFalseAndExpiresAtAfter(
            Long authUserId, LocalDateTime now);
    
    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.isVerified = true, t.verifiedAt = :verifiedAt WHERE t.token = :token")
    void markAsVerified(@Param("token") String token, @Param("verifiedAt") LocalDateTime verifiedAt);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now OR t.isVerified = true")
    void deleteExpiredOrVerifiedTokens(@Param("now") LocalDateTime now);
}
