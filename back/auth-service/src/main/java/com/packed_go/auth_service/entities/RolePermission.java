package com.packed_go.auth_service.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"role", "resource", "action"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "resource", length = 50, nullable = false)
    private String resource;

    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
