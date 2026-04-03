package com.packed_go.auth_service.services.impl;

import com.packed_go.auth_service.entities.AuthUser;
import com.packed_go.auth_service.entities.RolePermission;
import com.packed_go.auth_service.repositories.AuthUserRepository;
import com.packed_go.auth_service.repositories.RolePermissionRepository;
import com.packed_go.auth_service.services.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final AuthUserRepository authUserRepository;

    @Override
    public List<String> getUserPermissions(Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return getRolePermissions(user.getRole());
    }

    @Override
    public List<String> getRolePermissions(String role) {
        return rolePermissionRepository.findPermissionsByRole(role);
    }

    @Override
    public boolean hasPermission(Long userId, String resource, String action) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return rolePermissionRepository.existsByRoleAndResourceAndAction(
                user.getRole(), resource, action);
    }

    @Override
    public void addPermissionToRole(String role, String resource, String action) {
        if (!rolePermissionRepository.existsByRoleAndResourceAndAction(role, resource, action)) {
            RolePermission permission = RolePermission.builder()
                    .role(role)
                    .resource(resource)
                    .action(action)
                    .build();
            
            rolePermissionRepository.save(permission);
            //log.info("Added permission {}:{} to role {}", resource, action, role);
        }
    }

    @Override
    public void removePermissionFromRole(String role, String resource, String action) {
        rolePermissionRepository.deleteByRoleAndResourceAndAction(role, resource, action);
        //log.info("Removed permission {}:{} from role {}", resource, action, role);
    }

    @Override
    public void initializeDefaultPermissions() {
        //log.info("Initializing default permissions...");
        
        // Permisos para ADMIN
        addPermissionToRole("ADMIN", "events", "create");
        addPermissionToRole("ADMIN", "events", "read");
        addPermissionToRole("ADMIN", "events", "update");
        addPermissionToRole("ADMIN", "events", "delete");
        addPermissionToRole("ADMIN", "analytics", "read");
        addPermissionToRole("ADMIN", "qr", "validate");
        addPermissionToRole("ADMIN", "orders", "read");
        addPermissionToRole("ADMIN", "users", "read");
        
        // Permisos para CUSTOMER
        addPermissionToRole("CUSTOMER", "events", "read");
        addPermissionToRole("CUSTOMER", "orders", "create");
        addPermissionToRole("CUSTOMER", "orders", "read");
        addPermissionToRole("CUSTOMER", "profile", "read");
        addPermissionToRole("CUSTOMER", "profile", "update");
        
        //log.info("Default permissions initialized successfully");
    }
}