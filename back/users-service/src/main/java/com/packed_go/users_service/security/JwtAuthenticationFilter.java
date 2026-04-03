package com.packed_go.users_service.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro JWT para users-service
 * Valida tokens generados por auth-service y establece la autenticación
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getTokenFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenValidator.validateToken(jwt)) {
                String username = tokenValidator.getUsernameFromToken(jwt);
                Long userId = tokenValidator.getUserIdFromToken(jwt);
                String role = tokenValidator.getRoleFromToken(jwt);
                String email = tokenValidator.getEmailFromToken(jwt);
                List<String> authorities = tokenValidator.getAuthoritiesFromToken(jwt);

                // Create authorities list
                List<SimpleGrantedAuthority> grantedAuthorities = new java.util.ArrayList<>();
                
                // Always add the main role as an authority (e.g. ROLE_EMPLOYEE)
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                if (authorities != null && !authorities.isEmpty()) {
                    grantedAuthorities.addAll(authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList()));
                }

                // Create principal map expected by controllers
                Map<String, Object> principal = new HashMap<>();
                principal.put("username", username);
                principal.put("userId", userId);
                principal.put("role", role);
                principal.put("email", email);

                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set authentication for user: {} with role: {}", username, role);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
