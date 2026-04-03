package com.packed_go.auth_service.services;

import com.packed_go.auth_service.dto.request.AdminLoginRequest;
import com.packed_go.auth_service.dto.request.AdminRegistrationRequest;
import com.packed_go.auth_service.dto.request.ChangePasswordLoggedUserRequest;
import com.packed_go.auth_service.dto.request.ChangePasswordRequest;
import com.packed_go.auth_service.dto.request.CustomerLoginRequest;
import com.packed_go.auth_service.dto.request.CustomerRegistrationRequest;
import com.packed_go.auth_service.dto.request.EmployeeLoginRequest;
import com.packed_go.auth_service.dto.request.PasswordResetRequest;
import com.packed_go.auth_service.dto.request.UpdateAuthUserRequest;
import com.packed_go.auth_service.dto.response.AuthUserProfileResponse;
import com.packed_go.auth_service.dto.response.LoginResponse;
import com.packed_go.auth_service.dto.response.TokenValidationResponse;
import com.packed_go.auth_service.entities.AuthUser;

public interface AuthService {
    
    LoginResponse loginAdmin(AdminLoginRequest request, String ipAddress, String userAgent);
    
    LoginResponse loginCustomer(CustomerLoginRequest request, String ipAddress, String userAgent);
    
    LoginResponse loginEmployee(EmployeeLoginRequest request, String ipAddress, String userAgent);
    
    AuthUser registerCustomer(CustomerRegistrationRequest request);
    
    AuthUser registerAdmin(AdminRegistrationRequest request);
    
    TokenValidationResponse validateToken(String token);
    
    void logout(String token);
    
    void logoutAllSessions(Long userId);
    
    String refreshToken(String refreshToken);
    
    AuthUser findByUsername(String username);
    
    AuthUser findByEmail(String email);
    
    AuthUser findByDocument(Long document);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByDocument(Long document);
    
    java.util.Map<String, Object> verifyEmail(String token);
    
    void requestPasswordReset(PasswordResetRequest request);
    
    boolean resetPassword(ChangePasswordRequest request);
    
    AuthUserProfileResponse getUserProfile(Long userId);
    
    AuthUser updateUserProfile(Long userId, UpdateAuthUserRequest request);
    
    boolean changePasswordLoggedUser(Long userId, ChangePasswordLoggedUserRequest request);
    
    void resendVerificationEmail(String email);
}
