package com.packed_go.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateEmployeeResponse {
    
    private Long id;
    private String email;
    private String username;
    private Long document;
    private Long adminId;
    private Boolean isActive;
}
