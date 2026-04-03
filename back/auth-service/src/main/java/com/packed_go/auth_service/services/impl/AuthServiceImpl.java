package com.packed_go.auth_service.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.packed_go.auth_service.dto.request.AdminLoginRequest;
import com.packed_go.auth_service.dto.request.AdminRegistrationRequest;
import com.packed_go.auth_service.dto.request.ChangePasswordLoggedUserRequest;
import com.packed_go.auth_service.dto.request.ChangePasswordRequest;
import com.packed_go.auth_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.auth_service.dto.request.CustomerLoginRequest;
import com.packed_go.auth_service.dto.request.CustomerRegistrationRequest;
import com.packed_go.auth_service.dto.request.EmployeeLoginRequest;
import com.packed_go.auth_service.dto.request.PasswordResetRequest;
import com.packed_go.auth_service.dto.request.UpdateAuthUserRequest;
import com.packed_go.auth_service.dto.response.AuthUserProfileResponse;
import com.packed_go.auth_service.dto.response.LoginResponse;
import com.packed_go.auth_service.dto.response.TokenValidationResponse;
import com.packed_go.auth_service.dto.response.ValidateEmployeeResponse;
import com.packed_go.auth_service.entities.AuthUser;
import com.packed_go.auth_service.entities.EmailVerificationToken;
import com.packed_go.auth_service.entities.LoginAttempt;
import com.packed_go.auth_service.entities.PasswordRecoveryToken;
import com.packed_go.auth_service.entities.UserSession;
import com.packed_go.auth_service.exceptions.BadRequestException;
import com.packed_go.auth_service.exceptions.ResourceNotFoundException;
import com.packed_go.auth_service.exceptions.UnauthorizedException;
import com.packed_go.auth_service.repositories.AuthUserRepository;
import com.packed_go.auth_service.repositories.EmailVerificationTokenRepository;
import com.packed_go.auth_service.repositories.LoginAttemptRepository;
import com.packed_go.auth_service.repositories.PasswordRecoveryTokenRepository;
import com.packed_go.auth_service.repositories.RolePermissionRepository;
import com.packed_go.auth_service.repositories.UserSessionRepository;
import com.packed_go.auth_service.security.JwtTokenProvider;
import com.packed_go.auth_service.services.AuthService;
import com.packed_go.auth_service.services.EmailService;
import com.packed_go.auth_service.services.UsersServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsersServiceClient usersServiceClient;
    // TODO: Agregar cuando sea necesario mapear DTOs complejos
    // private final ModelMapper modelMapper;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;

    @Override
    public LoginResponse loginAdmin(AdminLoginRequest request, String ipAddress, String userAgent) {
        log.info("Admin login attempt for email: {}", request.getEmail());
        
        // Buscar usuario por email y tipo de login ADMIN
        log.info("Searching for admin user by email: {}", request.getEmail());
        AuthUser user = authUserRepository.findByEmailAndLoginType(request.getEmail(), "EMAIL")
            .orElseThrow(() -> {
                log.error("Admin user not found with email: {}", request.getEmail());
                recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "User not found");
                return new UnauthorizedException("Invalid credentials");
            });
        log.info("Admin user found with ID: {}", user.getId());

        // Verificar si es admin
        if (!"ADMIN".equals(user.getRole()) && !"SUPER_ADMIN".equals(user.getRole())) {
            log.error("User {} is not an admin", user.getId());
            recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "Not an admin");
            throw new UnauthorizedException("Access denied");
        }
        log.info("User {} is an admin", user.getId());

        // Verificar si la cuenta está bloqueada
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            log.error("Admin account is locked for user: {}", user.getId());
            recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "Account locked");
            throw new UnauthorizedException("Account is locked until " + user.getLockedUntil());
        }
        log.info("Admin account is not locked for user: {}", user.getId());

        // Verificar si el email está verificado
        if (!user.getIsEmailVerified()) {
            log.error("Email not verified for admin user: {}", user.getId());
            recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "Email not verified");
            throw new UnauthorizedException("Por favor verifica tu email antes de iniciar sesión. Revisa tu bandeja de entrada para encontrar el enlace de verificación.");
        }
        log.info("Email verified for admin user: {}", user.getId());

        // Verificar contraseña
        log.info("Verifying password for admin user: {}", user.getId());
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("Invalid password for admin user: {}", user.getId());
            handleFailedLogin(user, request.getEmail(), "EMAIL", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid credentials");
        }
        log.info("Password verified for admin user: {}", user.getId());

        // Login exitoso
        log.info("Processing successful login for admin user: {}", user.getId());
        return processSuccessfulLogin(user, ipAddress, userAgent);
    }

    @Override
    public LoginResponse loginCustomer(CustomerLoginRequest request, String ipAddress, String userAgent) {
        log.debug("Customer login attempt for document: {}", request.getDocument());
        
        // Buscar usuario por documento y tipo de login CUSTOMER
        AuthUser user = authUserRepository.findByDocumentAndLoginType(request.getDocument(), "DOCUMENT")
            .orElseThrow(() -> {
                recordFailedLogin(String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent, "User not found");
                return new UnauthorizedException("Invalid credentials");
            });

        // Verificar si es cliente
        if (!"CUSTOMER".equals(user.getRole())) {
            recordFailedLogin(String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent, "Not a customer");
            throw new UnauthorizedException("Access denied");
        }

        // Verificar si la cuenta está bloqueada
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            recordFailedLogin(String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent, "Account locked");
            throw new UnauthorizedException("Account is locked until " + user.getLockedUntil());
        }

        // Verificar si el email está verificado
        if (!user.getIsEmailVerified()) {
            log.error("Email not verified for customer user: {}", user.getId());
            recordFailedLogin(String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent, "Email not verified");
            throw new UnauthorizedException("Por favor verifica tu email antes de iniciar sesión. Revisa tu bandeja de entrada para encontrar el enlace de verificación.");
        }
        log.info("Email verified for customer user: {}", user.getId());

        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid credentials");
        }

        // Login exitoso
        return processSuccessfulLogin(user, ipAddress, userAgent);
    }

    @Override
    public LoginResponse loginEmployee(EmployeeLoginRequest request, String ipAddress, String userAgent) {
        log.info("Employee login attempt for email: {}", request.getEmail());
        
        // Validar credenciales con users-service
        ValidateEmployeeResponse employeeData;
        try {
            employeeData = usersServiceClient.validateEmployee(request.getEmail(), request.getPassword());
        } catch (Exception e) {
            log.error("Employee validation failed for email: {}", request.getEmail(), e);
            recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "Validation failed");
            throw new UnauthorizedException("Invalid credentials");
        }

        // Verificar que el empleado esté activo
        if (!employeeData.getIsActive()) {
            log.error("Employee account is inactive: {}", request.getEmail());
            recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "Account inactive");
            throw new UnauthorizedException("Tu cuenta ha sido desactivada. Por favor, contacta con el administrador.");
        }

        // Buscar usuario en auth-service por documento (llave única) o crear uno nuevo
        AuthUser user = authUserRepository.findByDocumentAndLoginType(employeeData.getDocument(), "EMAIL")
            .orElseGet(() -> {
                log.info("Creating new auth user for employee: {} (document: {})", request.getEmail(), employeeData.getDocument());
                AuthUser newUser = AuthUser.builder()
                    .email(employeeData.getEmail()) // Usar email del users-service
                    .username(employeeData.getUsername())
                    .document(employeeData.getDocument())
                    .passwordHash(request.getPassword()) // Ya viene hasheado del users-service
                    .role("EMPLOYEE")
                    .loginType("EMAIL")
                    .isEmailVerified(true) // Los empleados son creados por admins
                    .failedLoginAttempts(0)
                    .userProfileId(0L) // Default value for employees
                    .build();
                return authUserRepository.save(newUser);
            });

        // Verificar si la cuenta está bloqueada
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            log.error("Employee account is locked: {}", user.getId());
            recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "Account locked");
            throw new UnauthorizedException("Account is locked until " + user.getLockedUntil());
        }

        // Login exitoso
        log.info("Processing successful login for employee: {}", user.getId());
        return processSuccessfulLogin(user, ipAddress, userAgent);
    }

    @Override
    public AuthUser registerCustomer(CustomerRegistrationRequest request) {
        log.debug("Registering new customer with username: {}", request.getUsername());
        
        // Validar que no exista el username
        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        // Validar que no exista el email
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Validar que no exista el documento
        if (authUserRepository.existsByDocument(request.getDocument())) {
            throw new BadRequestException("Document already registered");
        }

        // Crear nuevo usuario
        AuthUser newUser = AuthUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .document(request.getDocument())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("CUSTOMER")
                .loginType("DOCUMENT")
                .isActive(true)
                .isEmailVerified(false) // ❌ Requiere verificación de email
                .isDocumentVerified(false)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userProfileId(0L) // Se actualizará cuando se cree el perfil en USER-SERVICE
                .build();

        AuthUser savedUser = authUserRepository.save(newUser);
        log.info("Customer registered successfully with ID: {}", savedUser.getId());
        
        // Llamar al users-service para crear el perfil de usuario
        try {
            CreateProfileFromAuthRequest profileRequest = CreateProfileFromAuthRequest.builder()
                    .authUserId(savedUser.getId())
                    .document(savedUser.getDocument())
                    .name(request.getName())
                    .lastName(request.getLastName())
                    .bornDate(request.getBornDate())
                    .telephone(request.getTelephone())
                    .gender(request.getGender())
                    .build();
            
            usersServiceClient.createUserProfile(profileRequest);
            log.info("User profile creation request sent for authUserId: {}", savedUser.getId());
            
        } catch (Exception e) {
            log.error("Failed to create user profile for authUserId: {}, continuing with registration", 
                     savedUser.getId(), e);
            // No lanzamos la excepción para que el registro en auth-service continúe
        }
        
        
        // ✅ Email de verificación activado
        try {
            sendVerificationEmail(savedUser);
            log.info("Verification email sent for user ID: {}", savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to send verification email for user ID: {}", savedUser.getId(), e);
            // No lanzamos la excepción para que el registro continúe
        }
        log.info("✅ Customer registered - Email verification required - ID: {}", savedUser.getId());
        
        return savedUser;
    }

    @Override
public java.util.Map<String, Object> verifyEmail(String token) {
    try {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenAndIsVerifiedFalse(token)
                .orElse(null);
        
        if (verificationToken == null) {
            log.warn("Verification token not found: {}", token);
            return java.util.Map.of("success", false, "message", "Token not found or already used");
        }
        
        if (verificationToken.isExpired()) {
            log.warn("Verification token expired: {}", token);
            return java.util.Map.of("success", false, "message", "Token expired");
        }
        
        // Marcar como verificado
        emailVerificationTokenRepository.markAsVerified(token, LocalDateTime.now());
        
        // Actualizar el usuario
        AuthUser user = authUserRepository.findById(verificationToken.getAuthUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        user.setIsEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        authUserRepository.save(user);
        
        log.info("Email verified successfully for user ID: {} with role: {}", user.getId(), user.getRole());
        
        return java.util.Map.of(
            "success", true,
            "message", "Email verified successfully",
            "role", user.getRole()
        );
        
    } catch (Exception e) {
        log.error("Error verifying email token: {}", token, e);
        return java.util.Map.of("success", false, "message", "Error verifying email");
    }
}

    private void sendVerificationEmail(AuthUser user) {
    // Generar token único
    String token = UUID.randomUUID().toString().replace("-", "");

    // Crear token de verificación con expiración de 24 horas
    EmailVerificationToken verificationToken = EmailVerificationToken.builder()
        .token(token)
        .authUserId(user.getId())
        .expiresAt(LocalDateTime.now().plusHours(24))
        .isVerified(false)
        .createdAt(LocalDateTime.now())
        .build();

    emailVerificationTokenRepository.save(verificationToken);

    // Enviar email
    String destinationEmail = user.getEmail();
    if ("ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
        destinationEmail = "packedgo.events@gmail.com";
    }
    emailService.sendVerificationEmail(destinationEmail, user.getUsername(), token);

    log.info("Email verification token generated for user ID: {} (to: {})", user.getId(), destinationEmail);
    }

    public AuthUser registerAdmin(AdminRegistrationRequest request) {
        log.debug("Registering new admin with username: {}", request.getUsername());
        
        // Validar código de autorización
        if (!"PACKEDGO-ADMIN-2025".equals(request.getAuthorizationCode())) {
            throw new BadRequestException("Invalid authorization code");
        }
        
        // Validar que no exista el username
        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        // Validar que no exista el email
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Crear nuevo admin
        AuthUser newAdmin = AuthUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .loginType("EMAIL")
                .isActive(true)
                .isEmailVerified(false) // ❌ Requiere verificación de email
                .isDocumentVerified(false)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userProfileId(0L)
                .build();

        AuthUser savedAdmin = authUserRepository.save(newAdmin);
        log.info("Admin registered successfully with ID: {}", savedAdmin.getId());
        
        // ✅ Email de verificación activado
        try {
            sendVerificationEmail(savedAdmin);
            log.info("Verification email sent for admin ID: {}", savedAdmin.getId());
        } catch (Exception e) {
            log.error("Failed to send verification email for admin ID: {}", savedAdmin.getId(), e);
            // No lanzamos la excepción para que el registro continúe
        }
        log.info("✅ Admin registered - Email verification required - ID: {}", savedAdmin.getId());
        
        return savedAdmin;
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        try {
            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                List<String> permissions = rolePermissionRepository.findPermissionsByRole(role);
                
                return TokenValidationResponse.builder()
                        .valid(true)
                        .userId(userId)
                        .role(role)
                        .permissions(permissions)
                        .build();
            }
        } catch (Exception e) {
            log.error("Token validation error", e);
        }
        
        return new TokenValidationResponse(false, "Invalid token");
    }

    @Override
    public void logout(String token) {
        if (token != null) {
            userSessionRepository.findBySessionToken(token).ifPresent(session -> {
                session.setIsActive(false);
                userSessionRepository.save(session);
            });
            log.info("User logged out successfully");
        }
    }

    @Override
    public void logoutAllSessions(Long userId) {
        userSessionRepository.deactivateAllUserSessions(userId);
        log.info("All sessions for user {} have been deactivated", userId);
    }

    @Override
    public String refreshToken(String refreshToken) {
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!session.getIsActive() || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        AuthUser user = authUserRepository.findById(session.getAuthUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> permissions = rolePermissionRepository.findPermissionsByRole(user.getRole());
        
        String newToken = jwtTokenProvider.generateTokenFromUserId(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole(), permissions);
        
        session.setSessionToken(newToken);
        session.setLastActivity(LocalDateTime.now());
        userSessionRepository.save(session);
        
        return newToken;
    }

    @Override
    public AuthUser findByUsername(String username) {
        return authUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public AuthUser findByEmail(String email) {
        return authUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public AuthUser findByDocument(Long document) {
        return authUserRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public boolean existsByUsername(String username) {
        return authUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return authUserRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByDocument(Long document) {
        return authUserRepository.existsByDocument(document);
    }

    @Override
    public void requestPasswordReset(PasswordResetRequest request) {
        log.info("Password reset request for email: {} and document: {}", request.getEmail(), request.getDocument());
        
        // Buscar usuario por email y documento para validar que coincidan
        AuthUser userByEmail = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        
        AuthUser userByDocument = authUserRepository.findByDocument(request.getDocument())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with document: " + request.getDocument()));
        
        // Verificar que el email y documento pertenezcan al mismo usuario
        if (!userByEmail.getId().equals(userByDocument.getId())) {
            throw new BadRequestException("Email and document do not match");
        }
        
        AuthUser user = userByEmail; // Usar cualquiera de los dos ya que son el mismo
        
        // Verificar si ya existe un token activo para este usuario
        Optional<PasswordRecoveryToken> existingToken = passwordRecoveryTokenRepository
                .findByAuthUserIdAndIsUsedFalseAndExpiresAtAfter(user.getId(), LocalDateTime.now());
        
        if (existingToken.isPresent()) {
            log.warn("Active password reset token already exists for user: {}", user.getId());
            throw new BadRequestException("Password reset request already exists. Check your email.");
        }
        
        // Generar nuevo token
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1); // Token válido por 1 hora
        
        PasswordRecoveryToken token = PasswordRecoveryToken.builder()
                .authUserId(user.getId())
                .token(resetToken)
                .expiresAt(expiresAt)
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        passwordRecoveryTokenRepository.save(token);
        
        // Enviar email de recuperación
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);
            log.info("Password reset email sent for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send password reset email for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Override
    public boolean resetPassword(ChangePasswordRequest request) {
        log.info("Password reset attempt with token");
        
        // Buscar token válido
        PasswordRecoveryToken token = passwordRecoveryTokenRepository
                .findByTokenAndIsUsedFalse(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        
        // Verificar si el token ha expirado
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }
        
        // Buscar el usuario
        AuthUser user = authUserRepository.findById(token.getAuthUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Cambiar la contraseña
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        authUserRepository.save(user);
        
        // Invalidar todas las sesiones activas del usuario por seguridad
        logoutAllSessions(user.getId());
        
        // Marcar el token como usado
        passwordRecoveryTokenRepository.markAsUsed(token.getToken(), LocalDateTime.now());
        
        log.info("Password reset successful for user: {} - All sessions invalidated", user.getId());
        return true;
    }

    @Override
    public AuthUserProfileResponse getUserProfile(Long userId) {
        log.info("Getting user profile for userId: {}", userId);
        
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        return AuthUserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .document(user.getDocument())
                .role(user.getRole())
                .loginType(user.getLoginType())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .isDocumentVerified(user.getIsDocumentVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    @Override
    public AuthUser updateUserProfile(Long userId, UpdateAuthUserRequest request) {
        log.info("Updating user profile for userId: {}", userId);
        
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Verificar si el nuevo username ya existe (y no es el mismo usuario)
        if (!user.getUsername().equals(request.getUsername()) && 
            authUserRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        
        // Verificar si el nuevo email ya existe (y no es el mismo usuario)
        if (!user.getEmail().equals(request.getEmail()) && 
            authUserRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        
        // Actualizar los datos
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setUpdatedAt(LocalDateTime.now());
        
        // Si cambió el email, marcar como no verificado
        if (!user.getEmail().equals(request.getEmail())) {
            user.setIsEmailVerified(false);
            // TODO: Enviar nuevo email de verificación
        }
        
        AuthUser updatedUser = authUserRepository.save(user);
        log.info("User profile updated successfully for userId: {}", userId);
        
        return updatedUser;
    }

    @Override
    public boolean changePasswordLoggedUser(Long userId, ChangePasswordLoggedUserRequest request) {
        log.info("Changing password for logged user: {}", userId);
        
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        
        // Cambiar la contraseña
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        authUserRepository.save(user);
        
        // Invalidar todas las sesiones activas del usuario por seguridad (excepto la actual)
        logoutAllSessions(user.getId());
        
        log.info("Password changed successfully for user: {} - All sessions invalidated", userId);
        return true;
    }

    // Métodos privados de ayuda
    
    private LoginResponse processSuccessfulLogin(AuthUser user, String ipAddress, String userAgent) {
        // Resetear intentos fallidos
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        authUserRepository.save(user);

        // Registrar login exitoso
        recordSuccessfulLogin(user.getUsername(), user.getLoginType(), ipAddress, userAgent);

        // Obtener permisos del usuario
        List<String> permissions = rolePermissionRepository.findPermissionsByRole(user.getRole());

        // Debug: verificar email
        log.info("DEBUG - Generando token para usuario: id={}, username={}, email={}, role={}", 
                user.getId(), user.getUsername(), user.getEmail(), user.getRole());

        // Generar tokens
        String token = jwtTokenProvider.generateTokenFromUserId(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole(), permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        // Crear sesión
        UserSession session = UserSession.builder()
                .authUserId(user.getId())
                .sessionToken(token)
                .refreshToken(refreshToken)
                .deviceInfo(extractDeviceInfo(userAgent))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationTime() / 1000))
                .lastActivity(LocalDateTime.now())
                .isActive(true)
                .build();

        userSessionRepository.save(session);

        // Construir respuesta
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .document(user.getDocument())
                .role(user.getRole())
                .loginType(user.getLoginType())
                .isEmailVerified(user.getIsEmailVerified())
                .lastLogin(user.getLastLogin())
                .build();

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userInfo)
                .permissions(permissions)
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .build();
    }

    private void handleFailedLogin(AuthUser user, String identifier, String loginType, 
                                 String ipAddress, String userAgent) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
            log.warn("Account locked for user: {} after {} failed attempts", identifier, attempts);
        }

        authUserRepository.save(user);
        recordFailedLogin(identifier, loginType, ipAddress, userAgent, "Invalid password");
    }

    private void recordFailedLogin(String identifier, String loginType, String ipAddress, 
                                 String userAgent, String reason) {
        LoginAttempt attempt = LoginAttempt.builder()
                .loginIdentifier(identifier)
                .loginType(loginType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(false)
                .failureReason(reason)
                .attemptedAt(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);
        log.warn("Failed login attempt for {}: {}", identifier, reason);
    }

    private void recordSuccessfulLogin(String identifier, String loginType, String ipAddress, String userAgent) {
        LoginAttempt attempt = LoginAttempt.builder()
                .loginIdentifier(identifier)
                .loginType(loginType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .attemptedAt(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);
        log.info("Successful login for {}", identifier);
    }

    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        // Extraer información bsica del dispositivo del User-Agent
        if (userAgent.contains("Mobile")) return "Mobile Device";
        if (userAgent.contains("Tablet")) return "Tablet";
        if (userAgent.contains("Windows")) return "Windows Desktop";
        if (userAgent.contains("Mac")) return "Mac Desktop";
        if (userAgent.contains("Linux")) return "Linux Desktop";
        
        return "Unknown Device";
    }

    @Override
    public void resendVerificationEmail(String email) {
        log.debug("Resending verification email to: {}", email);
        
        // Buscar usuario por email
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // Verificar si ya está verificado
        if (user.getIsEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        
        // Invalidar token anterior si existe (aunque no esté expirado)
        Optional<EmailVerificationToken> existingToken = 
                emailVerificationTokenRepository.findByAuthUserIdAndIsVerifiedFalseAndExpiresAtAfter(
                        user.getId(), LocalDateTime.now());
        
        if (existingToken.isPresent()) {
            EmailVerificationToken token = existingToken.get();
            token.setIsVerified(true);
            emailVerificationTokenRepository.save(token);
        }
        
        // Enviar nuevo email de verificación
        sendVerificationEmail(user);
        
        log.info("Verification email resent successfully to: {}", email);
    }
}