package com.packed_go.users_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.services.event-service.base-url:http://localhost:8083}")
    private String eventServiceBaseUrl;

    @Bean(name = "eventServiceWebClient")
    public WebClient eventServiceWebClient() {
        // Aumentar el límite del buffer para soportar respuestas grandes (eventos con nombres largos)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(5 * 1024 * 1024)) // 5 MB en lugar de 256 KB por defecto
                .build();

        return WebClient.builder()
                .baseUrl(eventServiceBaseUrl)
                .exchangeStrategies(strategies)
                .build();
    }
}
