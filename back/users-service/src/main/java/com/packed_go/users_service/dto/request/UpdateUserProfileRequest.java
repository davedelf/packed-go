package com.packed_go.users_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserProfileRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Last name is required") 
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;
    
    @NotNull(message = "Document is required")
    @Positive(message = "Document must be a positive number")
    private Long document;
    
    @NotNull(message = "Born date is required")
    @Past(message = "Born date must be in the past")
    private LocalDate bornDate;
    
    @NotNull(message = "Telephone is required")
    @Positive(message = "Telephone must be a positive number")
    private Long telephone;
    
    @Size(max = 255, message = "Profile image URL must not exceed 255 characters")
    private String profileImageUrl;
}