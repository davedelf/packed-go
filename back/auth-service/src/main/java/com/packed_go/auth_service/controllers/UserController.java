package com.packed_go.auth_service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.auth_service.dto.response.ApiResponse;
import com.packed_go.auth_service.services.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final AuthService authService;

    @GetMapping("/exists/username/{username}")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameExists(@PathVariable String username) {
        //log.debug("Checking if username exists: {}", username);
        
        boolean exists = authService.existsByUsername(username);
        
        return ResponseEntity.ok(ApiResponse.success(exists, "Username availability checked"));
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailExists(@PathVariable String email) {
        //log.debug("Checking if email exists: {}", email);
        
        boolean exists = authService.existsByEmail(email);
        
        return ResponseEntity.ok(ApiResponse.success(exists, "Email availability checked"));
    }

    @GetMapping("/exists/document/{document}")
    public ResponseEntity<ApiResponse<Boolean>> checkDocumentExists(@PathVariable Long document) {
        //log.debug("Checking if document exists: {}", document);
        
        boolean exists = authService.existsByDocument(document);
        
        return ResponseEntity.ok(ApiResponse.success(exists, "Document availability checked"));
    }
}