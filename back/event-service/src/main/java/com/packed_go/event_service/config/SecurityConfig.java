package com.packed_go.event_service.config;

import com.packed_go.event_service.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS manejado por API Gateway
            .cors(cors -> cors.disable())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos (públicos para consumers y otros servicios)
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/event-service/event").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/event-service/event/{id}").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/event-service/event/by-ids").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/event-service/consumption/categories").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/event-service/consumption/event/{eventId}").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/event-service/pass/event/{eventId}").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/event-service/pass/generate").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                
                // Endpoints internos entre servicios
                .requestMatchers("/internal/**").permitAll()
                
                // Todos los demás requieren autenticación
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}