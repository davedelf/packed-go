package com.packed_go.users_service.dto.request;

import com.packed_go.users_service.model.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProfileFromAuthRequest {
    
    @NotNull(message = "Auth user ID is required")
    private Long authUserId;

    @NotNull(message = "Document is required")
    private Long document;

    // Datos completos del perfil de usuario
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @NotNull(message = "Birth date is required")
    private LocalDate bornDate;

    @NotNull(message = "Telephone is required")
    private Long telephone;

    @NotNull(message = "Gender is required")
    private Gender gender;
}