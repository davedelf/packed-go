package com.packed_go.analytics_service.security;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Validador de tokens JWT generados por auth-service
 * Extrae información del usuario (userId, role, authorities) del token
 */
@Component
@Slf4j
public class JwtTokenValidator {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error validando token JWT: {}", e.getMessage());
            throw new RuntimeException("Token inválido o expirado");
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        Object userIdObj = claims.get("userId");
        
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        
        throw new RuntimeException("No se pudo extraer userId del token");
    }

    public String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return (String) claims.get("role");
    }

    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Authorization header inválido");
    }

    public boolean isAdmin(String token) {
        String role = getRoleFromToken(token);
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    public boolean canAccessUserResources(String token, Long targetUserId) {
        Long tokenUserId = getUserIdFromToken(token);
        return tokenUserId.equals(targetUserId) || isAdmin(token);
    }
}
