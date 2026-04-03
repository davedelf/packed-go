package com.packed_go.auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.services.users-service.base-url:http://localhost:8082}")
    private String usersServiceBaseUrl;

    @Bean
    public WebClient usersServiceWebClient() {
        return WebClient.builder()
                .baseUrl(usersServiceBaseUrl)
                .build();
    }
}