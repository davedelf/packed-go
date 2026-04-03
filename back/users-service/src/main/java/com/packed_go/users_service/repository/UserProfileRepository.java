package com.packed_go.users_service.repository;

import com.packed_go.users_service.entity.UserProfileEntity;
import com.packed_go.users_service.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
    Optional<UserProfileEntity> findById(Long id);
    Optional<UserProfileEntity> findByDocument(Long document);
    Optional<UserProfileEntity> findByAuthUserId(Long authUserId);
    boolean existsByAuthUserId(Long authUserId);
//    Optional<UserProfileEntity> findByEmail(String email);


    List<UserProfileEntity> findByIsActiveTrue();

//    Optional<UserProfileEntity> findByEmailAndIsActiveTrue(String email);

    Optional<UserProfileEntity> findByIdAndIsActiveTrue(Long id);

    Optional<UserProfileEntity> findByDocumentAndIsActiveTrue(Long document);
    Optional<UserProfileEntity> findByAuthUserIdAndIsActiveTrue(Long authUserId);
}
