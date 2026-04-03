package com.packed_go.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String refreshToken;
    private UserInfo user;
    private List<String> permissions;
    private Long expiresIn;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private Long document;
        private String role;
        private String loginType;
        private Boolean isEmailVerified;
        private LocalDateTime lastLogin;
        // No incluimos campos sensibles como passwordHash
    }
}
