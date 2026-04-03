package com.packed_go.users_service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class UserProfile {

    private Long id;
    private Long authUserId;
    private String name;
    private String lastName;
    private Gender gender;
    private Long document;
//    private String email;
    private LocalDate bornDate;
    private Long telephone;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive = true;

}
