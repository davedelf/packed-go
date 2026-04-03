package com.packed_go.users_service.config;

import com.packed_go.users_service.dto.UserProfileDTO;
import com.packed_go.users_service.entity.UserProfileEntity;
import com.packed_go.users_service.model.Gender;
import com.packed_go.users_service.model.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MappersConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // DTO -> Model (String gender → Enum)
        modelMapper.typeMap(UserProfileDTO.class, UserProfile.class)
                .addMappings(mapper -> mapper.map(
                        src -> src.getGender() != null ? Gender.valueOf(src.getGender().toUpperCase()) : null,
                        UserProfile::setGender
                ));

        // Model -> DTO (Enum → String)
        modelMapper.typeMap(UserProfile.class, UserProfileDTO.class)
                .addMappings(mapper -> mapper.map(
                        src -> src.getGender() != null ? src.getGender().name() : null,
                        UserProfileDTO::setGender
                ));

        // También podés mapear DTO -> Entity si lo usás directamente
        modelMapper.typeMap(UserProfileDTO.class, UserProfileEntity.class)
                .addMappings(mapper -> mapper.map(
                        src -> src.getGender() != null ? Gender.valueOf(src.getGender().toUpperCase()) : null,
                        UserProfileEntity::setGender
                ));

        return modelMapper;
    }

    @Bean
    @Qualifier("mergerMapper")
    public ModelMapper mergerMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setPropertyCondition(Conditions.isNotNull());
        return mapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
