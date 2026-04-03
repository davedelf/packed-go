package com.packed_go.users_service.service;

import com.packed_go.users_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.users_service.dto.request.UpdateUserProfileRequest;
import com.packed_go.users_service.dto.UserProfileDTO;
import com.packed_go.users_service.model.UserProfile;
import jakarta.transaction.Transactional;

import java.util.List;

public interface UserProfileService {
    UserProfile create(UserProfile model);

    UserProfile getById(Long id);

//    UserProfile getByEmail(String email);

    UserProfile getByDocument(Long document);
    
    UserProfile getByAuthUserId(Long authUserId);
    
    boolean existsByAuthUserId(Long authUserId);
    
    // Metodo actualizado para recibir todos los datos del perfil
    UserProfile createFromAuthService(CreateProfileFromAuthRequest request);

    List<UserProfile> getAll();

    UserProfile update(Long id, UserProfile model);
    
    UserProfile updateByAuthUserId(Long authUserId, UpdateUserProfileRequest request);

    void delete(Long id);

    @Transactional
    UserProfile deleteLogical(Long id);

    List<UserProfile> getAllActive();

//    UserProfile getByEmailActive(String email);

    UserProfile getByIdActive(Long id);

    UserProfile getByDocumentActive(Long document);
    
    UserProfile getByAuthUserIdActive(Long authUserId);
}