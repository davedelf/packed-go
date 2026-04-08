package com.packed_go.event_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter para event-service
 * Valida tokens JWT y establece la autenticación en el SecurityContext
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip JWT filter for public endpoints
        return path.contains("/event-service/event") && request.getMethod().equals("GET")
            || path.contains("/event-service/consumption")
            || path.contains("/event-service/pass")
            || path.contains("/actuator")
            || path.contains("/internal");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (jwtTokenValidator.validateToken(token)) {
                Long userId = jwtTokenValidator.getUserIdFromToken(token);
                String role = jwtTokenValidator.getRoleFromToken(token);
                List<String> authorities = jwtTokenValidator.getAuthoritiesFromToken(token);
                
                List<SimpleGrantedAuthority> grantedAuthorities = authorities != null
                        ? authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                        : List.of(new SimpleGrantedAuthority("ROLE_" + role));
                
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userId, null, grantedAuthorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("User {} authenticated with role {}", userId, role);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
