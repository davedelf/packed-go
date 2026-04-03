package com.packed_go.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserProfileResponse {
    
    private Long id;
    private String username;
    private String email;
    private Long document;
    private String role;
    private String loginType;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Boolean isDocumentVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}