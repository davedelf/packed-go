package com.packed_go.auth_service.repositories;

import com.packed_go.auth_service.entities.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByUsername(String username);
    
    Optional<AuthUser> findByEmail(String email);
    
    Optional<AuthUser> findByDocument(Long document);
    
    Optional<AuthUser> findByEmailAndLoginType(String email, String loginType);
    
    Optional<AuthUser> findByDocumentAndLoginType(Long document, String loginType);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByDocument(Long document);
    
    @Modifying
    @Query("UPDATE AuthUser u SET u.failedLoginAttempts = :attempts WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") Long userId, @Param("attempts") Integer attempts);
    
    @Modifying
    @Query("UPDATE AuthUser u SET u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void updateLockedUntil(@Param("userId") Long userId, @Param("lockedUntil") LocalDateTime lockedUntil);
    
    @Modifying
    @Query("UPDATE AuthUser u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);
    
    @Modifying
    @Query("UPDATE AuthUser u SET u.isEmailVerified = true WHERE u.id = :userId")
    void verifyEmail(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE AuthUser u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
}
