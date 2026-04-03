package com.packed_go.auth_service.services;

import java.util.List;

public interface PermissionService {
    
    List<String> getUserPermissions(Long userId);
    
    List<String> getRolePermissions(String role);
    
    boolean hasPermission(Long userId, String resource, String action);
    
    void addPermissionToRole(String role, String resource, String action);
    
    void removePermissionFromRole(String role, String resource, String action);
    
    void initializeDefaultPermissions();
}