package com.packed_go.users_service.dto;

import com.packed_go.users_service.model.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
@Getter
@Setter
public class UserProfileDTO {
/*
    private Long id;              // 👈 agregar
*/
    private Long authUserId;
    private String name;
    private String lastName;
    private String gender;        // se mantiene como String
    private Long document;
    private LocalDate bornDate;
    private Long telephone;
    private String profileImageUrl;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
