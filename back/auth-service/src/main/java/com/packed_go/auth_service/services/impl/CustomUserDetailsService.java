package com.packed_go.auth_service.services.impl;

import com.packed_go.auth_service.entities.AuthUser;
import com.packed_go.auth_service.repositories.AuthUserRepository;
import com.packed_go.auth_service.security.CustomUserDetails;
import com.packed_go.auth_service.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;
    private final PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser user = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return createUserDetails(user);
    }

    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return createUserDetails(user);
    }

    public UserDetails loadUserByDocument(Long document) throws UsernameNotFoundException {
        AuthUser user = authUserRepository.findByDocument(document)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with document: " + document));

        return createUserDetails(user);
    }

    private CustomUserDetails createUserDetails(AuthUser user) {
        // Verificar si la cuenta está bloqueada
        boolean isAccountNonLocked = user.getLockedUntil() == null || 
                                   user.getLockedUntil().isBefore(LocalDateTime.now());

        // Obtener permisos del usuario
        List<String> permissions = permissionService.getUserPermissions(user.getId());

        return CustomUserDetails.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .document(user.getDocument())
                .password(user.getPasswordHash())
                .role(user.getRole())
                .loginType(user.getLoginType())
                .permissions(permissions)
                .isActive(user.getIsActive() && isAccountNonLocked)
                .isEmailVerified(user.getIsEmailVerified())
                .build();
    }
}