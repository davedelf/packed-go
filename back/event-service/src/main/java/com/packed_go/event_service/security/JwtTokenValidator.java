package com.packed_go.event_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * JWT Utility compartida entre microservicios
 * Permite validar tokens generados por auth-service
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtTokenValidator {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("userId", Long.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getAuthoritiesFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("authorities", List.class);
    }

    /**
     * Extrae el token del header Authorization
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Valida si el usuario del token puede acceder a recursos creados por otro userId
     */
    public boolean canAccessUserResources(String token, Long requestedUserId) {
        try {
            Long tokenUserId = getUserIdFromToken(token);
            String role = getRoleFromToken(token);
            
            // El usuario puede acceder a sus propios recursos
            if (tokenUserId.equals(requestedUserId)) {
                return true;
            }
            
            // Los administradores pueden acceder a todos los recursos (si necesitas esta lógica)
            if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error validating user access: {}", e.getMessage());
            return false;
        }
    }
}
