package com.packed_go.users_service.service.impl;

import com.packed_go.users_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.users_service.dto.request.UpdateUserProfileRequest;
import com.packed_go.users_service.entity.UserProfileEntity;
import com.packed_go.users_service.model.UserProfile;
import com.packed_go.users_service.model.Gender;
import com.packed_go.users_service.repository.UserProfileRepository;
import com.packed_go.users_service.service.UserProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private final UserProfileRepository userProfileRepository;
    @Autowired
    private final ModelMapper modelMapper;


    @Override
    public UserProfile create(UserProfile model) {
        // Comprueba si ya existe un perfil con el mismo documento
        Optional<UserProfileEntity> userExist = userProfileRepository.findByDocument(model.getDocument());

        if (userExist.isPresent()) {
            // Si el usuario existe, lanza una excepción clara
            throw new RuntimeException("El perfil de usuario con documento " + model.getDocument() + " ya existe.");
        } else {
            // Mapea el DTO a la entidad
            UserProfileEntity userProfileEntity = modelMapper.map(model, UserProfileEntity.class);

            // Guarda la nueva entidad en la base de datos
            userProfileEntity.setId(null);
            UserProfileEntity userProfileEntitySaved = userProfileRepository.save(userProfileEntity);

            // Mapea la entidad guardada de vuelta a un DTO y lo retorna
            return modelMapper.map(userProfileEntitySaved, UserProfile.class);
        }
    }

    @Override
    public UserProfile getById(Long id) {
        Optional<UserProfileEntity> userExist = userProfileRepository.findById(id);
        if (userExist.isPresent()) {
            return modelMapper.map(userExist.get(), UserProfile.class);
        }else {
            throw new RuntimeException("UserProfile con id " + id + " no encontrado");
        }

    }

//    @Override
//    public UserProfile getByEmail(String email) {
//        Optional<UserProfileEntity> userExist = userProfileRepository.findByEmail(email);
//        if (!userExist.isEmpty()) {
//            return modelMapper.map(userExist, UserProfile.class);
//        }else {
//            throw new RuntimeException("UserProfile con email " + email + " no encontrado");
//        }
//    }

    @Override
    public UserProfile getByDocument(Long document) {
        Optional<UserProfileEntity> userExist = userProfileRepository.findByDocument(document);
        if (userExist.isPresent()) {
            return modelMapper.map(userExist.get(), UserProfile.class);
        }else {
            throw new RuntimeException("UserProfile con documento " + document + " no encontrado");
        }
    }

    @Override
    public UserProfile getByAuthUserId(Long authUserId) {
        Optional<UserProfileEntity> userExist = userProfileRepository.findByAuthUserId(authUserId);
        if (userExist.isPresent()) {
            return modelMapper.map(userExist.get(), UserProfile.class);
        } else {
            throw new RuntimeException("UserProfile con authUserId " + authUserId + " no encontrado");
        }
    }

    @Override
    public boolean existsByAuthUserId(Long authUserId) {
        return userProfileRepository.existsByAuthUserId(authUserId);
    }

    @Override
    public UserProfile createFromAuthService(CreateProfileFromAuthRequest request) {
        // Verificar si ya existe un perfil para este authUserId usando find
        Optional<UserProfileEntity> existingProfile = userProfileRepository.findByAuthUserId(request.getAuthUserId());
        log.info("Checking for existing profile with authUserId {}: {}", 
                request.getAuthUserId(), existingProfile.isPresent() ? "FOUND" : "NOT FOUND");
        
        if (existingProfile.isPresent()) {
            throw new RuntimeException("Ya existe un perfil para el usuario con authUserId: " + request.getAuthUserId());
        }

        // Crear nuevo perfil con todos los datos reales del request
        UserProfile userProfile = new UserProfile();
        userProfile.setAuthUserId(request.getAuthUserId());
        userProfile.setDocument(request.getDocument());
        
        // Usar datos reales en lugar de placeholders
        userProfile.setName(request.getName());
        userProfile.setLastName(request.getLastName());
        userProfile.setBornDate(request.getBornDate());
        userProfile.setTelephone(request.getTelephone());
        userProfile.setGender(request.getGender());
        
        // Solo estos campos tienen valores por defecto
        userProfile.setProfileImageUrl("");
        userProfile.setIsActive(true);

        return create(userProfile);
    }

    @Override
    public List<UserProfile> getAll() {
        List<UserProfileEntity> userProfileEntities = userProfileRepository.findAll();

        return userProfileEntities.stream()
                .map(entity -> modelMapper.map(entity, UserProfile.class))
                .toList();
    }

    @Override
    public UserProfile update(Long id, UserProfile model) {

        Optional<UserProfileEntity> userExist = userProfileRepository.findByIdAndIsActiveTrue(id);

        if (userExist.isPresent()) {
            UserProfileEntity entity = modelMapper.map(model,UserProfileEntity.class);
            entity.setId(id);
            UserProfileEntity updatedEntity = userProfileRepository.save(entity);

            return modelMapper.map(updatedEntity, UserProfile.class);
        } else {

            throw new RuntimeException("UserProfile activo con id " + id + " no existe");
        }
    }

    @Override
    public UserProfile updateByAuthUserId(Long authUserId, UpdateUserProfileRequest request) {
        Optional<UserProfileEntity> userExist = userProfileRepository.findByAuthUserIdAndIsActiveTrue(authUserId);

        if (userExist.isPresent()) {
            UserProfileEntity entity = userExist.get();
            
            // Validar que el documento no este en uso por otro usuario (si cambio)
            if (!entity.getDocument().equals(request.getDocument())) {
                Optional<UserProfileEntity> documentExists = userProfileRepository.findByDocument(request.getDocument());
                if (documentExists.isPresent() && !documentExists.get().getAuthUserId().equals(authUserId)) {
                    throw new RuntimeException("El documento " + request.getDocument() + " ya esta en uso por otro usuario");
                }
            }
            
            // Actualizar los campos
            entity.setName(request.getName());
            entity.setLastName(request.getLastName());
            entity.setGender(Gender.valueOf(request.getGender()));
            entity.setDocument(request.getDocument());
            entity.setBornDate(request.getBornDate());
            entity.setTelephone(request.getTelephone());
            entity.setProfileImageUrl(request.getProfileImageUrl());
            
            UserProfileEntity updatedEntity = userProfileRepository.save(entity);
            return modelMapper.map(updatedEntity, UserProfile.class);
        } else {
            throw new RuntimeException("UserProfile activo con authUserId " + authUserId + " no encontrado");
        }
    }


    @Override
    public void delete(Long id) {
        // Delete físico
        if (userProfileRepository.existsById(id)) {
            userProfileRepository.deleteById(id);
        } else {
            throw new RuntimeException("UserProfile con id " + id + " no encontrado");
        }
    }

    /**
     * Delete lógico: marca isActive = false
     */
    @Transactional
    @Override
    public UserProfile deleteLogical(Long id) {
        Optional<UserProfileEntity> userExist = userProfileRepository.findById(id);
        if (userExist.isPresent()) {
            UserProfileEntity entity = userExist.get();
            entity.setIsActive(false);
            UserProfileEntity updatedEntity = userProfileRepository.save(entity);
            return modelMapper.map(updatedEntity, UserProfile.class);
        } else {
            throw new RuntimeException("UserProfile con id " + id + " no encontrado");
        }
    }

    @Override
    public List<UserProfile> getAllActive() {
        List<UserProfileEntity> userProfileEntities = userProfileRepository.findByIsActiveTrue();
        return userProfileEntities.stream()
                .map(entity -> modelMapper.map(entity, UserProfile.class))
                .toList();
    }

//    @Override
//    public UserProfile getByEmailActive(String email) {
//        Optional<UserProfileEntity> userExist = userProfileRepository.findByEmailAndIsActiveTrue(email);
//        if (userExist.isPresent()) {
//            return modelMapper.map(userExist.get(), UserProfile.class);
//        } else {
//            throw new RuntimeException("UserProfile activo con email " + email + " no encontrado");
//        }
//    }
    @Override
    public UserProfile getByIdActive(Long id) {
        Optional<UserProfileEntity> userExist = userProfileRepository.findByIdAndIsActiveTrue(id);
        if (userExist.isPresent()) {
            return modelMapper.map(userExist.get(), UserProfile.class);
        } else {
            throw new RuntimeException("UserProfile activo con id " + id + " no encontrado");
        }
    }
    @Override
    public UserProfile getByDocumentActive(Long document) {
        Optional<UserProfileEntity> userExist = userProfileRepository.findByDocumentAndIsActiveTrue(document);
        if (userExist.isPresent()) {
            return modelMapper.map(userExist.get(), UserProfile.class);
        } else {
            throw new RuntimeException("UserProfile activo con documento " + document + " no encontrado");
        }
    }

    @Override
    public UserProfile getByAuthUserIdActive(Long authUserId) {
        Optional<UserProfileEntity> userExist = userProfileRepository.findByAuthUserIdAndIsActiveTrue(authUserId);
        if (userExist.isPresent()) {
            return modelMapper.map(userExist.get(), UserProfile.class);
        } else {
            throw new RuntimeException("UserProfile activo con authUserId " + authUserId + " no encontrado");
        }
    }

}
