package com.packed_go.order_service.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura estándar de respuesta de error
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private LocalDateTime timestamp;
    
    private Integer status;
    
    private String error;
    
    private String message;
    
    private Map<String, String> validationErrors; // Para errores de validación
}
