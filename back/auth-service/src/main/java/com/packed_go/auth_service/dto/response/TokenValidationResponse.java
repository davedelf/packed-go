package com.packed_go.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationResponse {

    private boolean valid;
    private Long userId;
    private String role;
    private List<String> permissions;
    private String message;

    // Constructor para respuestas de error
    public TokenValidationResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    // Constructor para respuestas exitosas
    public TokenValidationResponse(boolean valid, Long userId, String role, List<String> permissions) {
        this.valid = valid;
        this.userId = userId;
        this.role = role;
        this.permissions = permissions;
    }
}
