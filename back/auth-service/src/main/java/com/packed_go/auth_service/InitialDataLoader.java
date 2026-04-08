package com.packed_go.auth_service;

import com.packed_go.auth_service.entities.AuthUser;
import com.packed_go.auth_service.repositories.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataLoader para crear usuarios iniciales del sistema.
 * Se ejecuta al iniciar la aplicación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitialDataLoader implements CommandLineRunner {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    // Credenciales del SUPER_ADMIN inicial
    private static final String SUPER_ADMIN_EMAIL = "superadmin@packedgo.com";
    private static final String SUPER_ADMIN_PASSWORD = "SuperAdmin2025!";
    private static final String SUPER_ADMIN_CODE = "PACKEDGO-SUPERADMIN-2025";

    @Override
    public void run(String... args) {
        // Verificar si ya existe un SUPER_ADMIN
        if (authUserRepository.findByEmail(SUPER_ADMIN_EMAIL).isEmpty()) {
            AuthUser superAdmin = AuthUser.builder()
                    .username("superadmin")
                    .email(SUPER_ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(SUPER_ADMIN_PASSWORD))
                    .role("SUPER_ADMIN")
                    .loginType("EMAIL")
                    .isActive(true)
                    .isEmailVerified(true)
                    .isDocumentVerified(true)
                    .userProfileId(1L)
                    .build();

            authUserRepository.save(superAdmin);
            log.info("✅ SUPER_ADMIN created: {} / {}", SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD);
            log.info("🔑 Código de autorización para registrar admins: {}", SUPER_ADMIN_CODE);
        } else {
            log.info("ℹ️ SUPER_ADMIN already exists");
        }

        // Verificar si ya existe el admin de prueba
        if (authUserRepository.findByEmail("admin@test.com").isEmpty()) {
            AuthUser admin = AuthUser.builder()
                    .username("admin")
                    .email("admin@test.com")
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .role("ADMIN")
                    .loginType("EMAIL")
                    .isActive(true)
                    .isEmailVerified(true)
                    .isDocumentVerified(true)
                    .userProfileId(2L)
                    .build();

            authUserRepository.save(admin);
            log.info("✅ ADMIN created: admin@test.com / Admin123!");
        }

        // Verificar si ya existe el empleado de prueba
        if (authUserRepository.findByEmail("employee@test.com").isEmpty()) {
            AuthUser employee = AuthUser.builder()
                    .username("employee")
                    .email("employee@test.com")
                    .passwordHash(passwordEncoder.encode("Employee123!"))
                    .role("EMPLOYEE")
                    .loginType("EMAIL")
                    .isActive(true)
                    .isEmailVerified(true)
                    .isDocumentVerified(true)
                    .userProfileId(3L)
                    .build();

            authUserRepository.save(employee);
            log.info("✅ EMPLOYEE created: employee@test.com / Employee123!");
        }

        // Verificar si ya existe el cliente de prueba
        if (authUserRepository.findByDocument(12345678L).isEmpty()) {
            AuthUser customer = AuthUser.builder()
                    .username("customer")
                    .email("customer@test.com")
                    .document(12345678L)
                    .passwordHash(passwordEncoder.encode("Customer123!"))
                    .role("CUSTOMER")
                    .loginType("DOCUMENT")
                    .isActive(true)
                    .isEmailVerified(true)
                    .isDocumentVerified(true)
                    .userProfileId(4L)
                    .build();

            authUserRepository.save(customer);
            log.info("✅ CUSTOMER created: 12345678 / Customer123!");
        }
    }
}
