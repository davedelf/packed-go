package com.packed_go.auth_service.repositories;

import com.packed_go.auth_service.entities.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole(String role);
    
    @Query("SELECT DISTINCT CONCAT(rp.resource, ':', rp.action) FROM RolePermission rp WHERE rp.role = :role")
    List<String> findPermissionsByRole(@Param("role") String role);
    
    boolean existsByRoleAndResourceAndAction(String role, String resource, String action);
    
    void deleteByRoleAndResourceAndAction(String role, String resource, String action);
}
