package com.packed_go.auth_service.services.impl;

import com.packed_go.auth_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.auth_service.dto.response.ValidateEmployeeResponse;
import com.packed_go.auth_service.services.UsersServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersServiceClientImpl implements UsersServiceClient {

    @Qualifier("usersServiceWebClient")
    private final WebClient webClient;

    @Override
    public void createUserProfile(CreateProfileFromAuthRequest request) {
        try {
            log.info("Calling users-service to create profile for authUserId: {}", request.getAuthUserId());
            
            webClient.post()
                    .uri("/api/user-profiles/from-auth")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Bloquear para hacer la llamada síncrona
                    
            log.info("Successfully created user profile for authUserId: {}", request.getAuthUserId());
            
        } catch (Exception e) {
            log.error("Failed to create user profile for authUserId: {}", request.getAuthUserId(), e);
            // No lanzamos la excepción para que el registro en auth-service continúe
            // En un entorno productivo podrías implementar retry logic o dead letter queue
        }
    }

    @Override
    public ValidateEmployeeResponse validateEmployee(String email, String password) {
        try {
            log.info("Calling users-service to validate employee: {}", email);
            
            Map<String, String> request = new HashMap<>();
            request.put("email", email);
            request.put("password", password);
            
            ValidateEmployeeResponse response = webClient.post()
                    .uri("/api/internal/employees/validate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ValidateEmployeeResponse.class)
                    .block();
                    
            log.info("Successfully validated employee: {}", email);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to validate employee: {}", email, e);
            throw new RuntimeException("Employee validation failed", e);
        }
    }
}