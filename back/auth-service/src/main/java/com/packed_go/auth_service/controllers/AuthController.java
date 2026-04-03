package com.packed_go.auth_service.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.packed_go.auth_service.dto.request.AdminLoginRequest;
import com.packed_go.auth_service.dto.request.AdminRegistrationRequest;
import com.packed_go.auth_service.dto.request.ChangePasswordLoggedUserRequest;
import com.packed_go.auth_service.dto.request.ChangePasswordRequest;
import com.packed_go.auth_service.dto.request.CustomerLoginRequest;
import com.packed_go.auth_service.dto.request.CustomerRegistrationRequest;
import com.packed_go.auth_service.dto.request.EmployeeLoginRequest;
import com.packed_go.auth_service.dto.request.LogoutRequest;
import com.packed_go.auth_service.dto.request.PasswordResetRequest;
import com.packed_go.auth_service.dto.request.ResendVerificationRequest;
import com.packed_go.auth_service.dto.request.TokenValidationRequest;
import com.packed_go.auth_service.dto.request.UpdateAuthUserRequest;
import com.packed_go.auth_service.dto.response.ApiResponse;
import com.packed_go.auth_service.dto.response.AuthUserProfileResponse;
import com.packed_go.auth_service.dto.response.LoginResponse;
import com.packed_go.auth_service.dto.response.TokenValidationResponse;
import com.packed_go.auth_service.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j

public class AuthController {

    private final AuthService authService;

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest) {
        
        //log.info("Admin login attempt for email: {}", request.getEmail());
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.loginAdmin(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Admin login successful"));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<ApiResponse<LoginResponse>> customerLogin(
            @Valid @RequestBody CustomerLoginRequest request,
            HttpServletRequest httpRequest) {
        
        //log.info("Customer login attempt for document: {}", request.getDocument());
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.loginCustomer(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Customer login successful"));
    }

    @PostMapping("/employee/login")
    public ResponseEntity<ApiResponse<LoginResponse>> employeeLogin(
            @Valid @RequestBody EmployeeLoginRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Employee login attempt for email: {}", request.getEmail());
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.loginEmployee(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Employee login successful"));
    }

    @PostMapping("/customer/register")
    public ResponseEntity<ApiResponse<String>> registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request) {
        
        log.info("Customer registration attempt for document: {}", request.getDocument());
        
        authService.registerCustomer(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered successfully. Please verify your email."));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<ApiResponse<String>> registerAdmin(
            @Valid @RequestBody AdminRegistrationRequest request) {
        
        log.info("Admin registration attempt for email: {}", request.getEmail());
        
        authService.registerAdmin(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Admin registration request received. Awaiting approval."));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @Valid @RequestBody TokenValidationRequest request) {
        
        log.debug("Token validation request");
        
        TokenValidationResponse response = authService.validateToken(request.getToken());
        
        return ResponseEntity.ok(ApiResponse.success(response, "Token validation completed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @Valid @RequestBody LogoutRequest request) {
        
        log.info("Logout request");
        
        authService.logout(request.getToken());
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> verifyEmail(
            @RequestParam("token") String token) {
        
        log.info("Email verification request for token: {}", token.substring(0, Math.min(token.length(), 8)) + "...");
        
        java.util.Map<String, Object> result = authService.verifyEmail(token);
        boolean verified = (boolean) result.get("success");
        
        if (verified) {
            return ResponseEntity.ok(ApiResponse.success(result, (String) result.get("message")));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error((String) result.get("message")));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @RequestBody String refreshToken) {
        
        log.info("Token refresh request");
        
        String newToken = authService.refreshToken(refreshToken);
        
        return ResponseEntity.ok(ApiResponse.success(newToken, "Token refreshed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        
        log.info("Password reset request for email: {}", request.getEmail());
        
        try {
            authService.requestPasswordReset(request);
            return ResponseEntity.ok(ApiResponse.success("Password reset email sent if email exists"));
        } catch (Exception e) {
            // Por seguridad, no revelamos si el email existe o no
            log.error("Password reset request failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success("Password reset email sent if email exists"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        
        log.info("Password reset attempt");
        
        boolean reset = authService.resetPassword(request);
        
        if (reset) {
            return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to reset password"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<AuthUserProfileResponse>> getUserProfile(
            @PathVariable Long userId) {
        
        log.info("Getting user profile for userId: {}", userId);
        
        AuthUserProfileResponse profile = authService.getUserProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.success(profile, "User profile retrieved successfully"));
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<String>> updateUserProfile(
            @PathVariable Long userId, 
            @Valid @RequestBody UpdateAuthUserRequest request) {
        
        log.info("Updating user profile for userId: {}", userId);
        
        authService.updateUserProfile(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully"));
    }

    @PostMapping("/change-password/{userId}")
    public ResponseEntity<ApiResponse<String>> changePasswordLoggedUser(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordLoggedUserRequest request) {
        
        log.info("Changing password for logged user: {}", userId);
        
        boolean changed = authService.changePasswordLoggedUser(userId, request);
        
        if (changed) {
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to change password"));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request) {
        
        log.info("Resending verification email to: {}", request.getEmail());
        
        authService.resendVerificationEmail(request.getEmail());
        
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email resent successfully"));
    }

    // Métodos de utilidad
    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}