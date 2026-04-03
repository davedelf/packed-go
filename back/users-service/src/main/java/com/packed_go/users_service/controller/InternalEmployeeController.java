package com.packed_go.users_service.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.users_service.dto.EmployeeDTO.ValidateEmployeeResponse;
import com.packed_go.users_service.entity.Employee;
import com.packed_go.users_service.service.EmployeeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/internal/employees")
@RequiredArgsConstructor
@Slf4j
public class InternalEmployeeController {
    
    private final EmployeeService employeeService;
    
    /**
     * Internal endpoint for auth-service to validate employee credentials
     * This endpoint is NOT exposed to external clients
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidateEmployeeResponse> validateEmployee(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        log.info("Validating employee credentials for email: {}", email);
        
        // Find employee by email
        Employee employee;
        try {
            employee = employeeService.findByEmail(email);
        } catch (IllegalArgumentException e) {
            log.error("Employee not found: {}", email);
            return ResponseEntity.status(401).build();
        }
        
        log.info("Employee found: ID={}, Hash={}", employee.getId(), employee.getPasswordHash());
        log.info("Received password length: {}", (password != null ? password.length() : "null"));

        // Validate password
        boolean isValid = employeeService.validatePassword(employee, password);
        log.info("Password validation result: {}", isValid);

        if (!isValid) {
            log.error("Invalid password for employee: {}", email);
            return ResponseEntity.status(401).build();
        }
        
        // Build response
        ValidateEmployeeResponse response = ValidateEmployeeResponse.builder()
            .id(employee.getId())
            .email(employee.getEmail())
            .username(employee.getUsername())
            .document(employee.getDocument())
            .adminId(employee.getAdminId())
            .isActive(employee.getIsActive())
            .build();
        
        log.info("Successfully validated employee: {}", email);
        return ResponseEntity.ok(response);
    }
}
