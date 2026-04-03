package com.packed_go.order_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.services.event-service.base-url:http://localhost:8086/api}")
    private String eventServiceBaseUrl;

    @Value("${app.services.auth-service.base-url:http://localhost:8081/api}")
    private String authServiceBaseUrl;

    /**
     * WebClient para comunicarse con EVENT-SERVICE
     */
    @Bean
    public WebClient eventServiceWebClient() {
        return WebClient.builder()
                .baseUrl(eventServiceBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB buffer
                .build();
    }

    /**
     * WebClient para comunicarse con AUTH-SERVICE (si es necesario)
     */
    @Bean
    public WebClient authServiceWebClient() {
        return WebClient.builder()
                .baseUrl(authServiceBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * WebClient genérico con configuración común
     * Útil para servicios adicionales en el futuro
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024));
    }

    /**
     * RestTemplate para comunicación con payment-service
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
