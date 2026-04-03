package com.packed_go.api_gateway.filter;

import com.packed_go.api_gateway.config.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            log.info("🔐 AuthenticationFilter - Path: {}", request.getPath());

            // SECURITY FIX: Remove any client-sent X-User-* headers to prevent spoofing
            // Only the Gateway should inject these headers from validated JWT
            ServerHttpRequest.Builder requestBuilder = request.mutate();
            requestBuilder.removeHeader("X-User-Id");
            requestBuilder.removeHeader("X-User-Role");
            requestBuilder.removeHeader("x-user-id");
            requestBuilder.removeHeader("x-user-role");
            ServerHttpRequest cleanedRequest = requestBuilder.build();

            // Check if Authorization header is present (use original request to get headers before cleanup)
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("⚠️ Missing Authorization header for path: {}", request.getPath());
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("⚠️ Invalid Authorization header format for path: {}", request.getPath());
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Validate token
            try {
                if (!jwtUtil.validateToken(token)) {
                    log.warn("⚠️ Invalid or expired token for path: {}", request.getPath());
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }

                // Extract user information and add to headers for downstream services
                String userId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                log.info("✅ Token validated - UserId: {}, Role: {}", userId, role);

                // Add user information to request headers
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("❌ Error validating token: {}", e.getMessage());
                return onError(exchange, "Token validation error", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String errorResponse = String.format("{\"error\": \"%s\", \"status\": %d}", message, status.value());
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes()))
        );
    }

    public static class Config {
        // Configuration properties if needed
    }
}
