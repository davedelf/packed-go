package com.packed_go.auth_service.services;

import com.packed_go.auth_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.auth_service.dto.response.ValidateEmployeeResponse;

public interface UsersServiceClient {
    
    void createUserProfile(CreateProfileFromAuthRequest request);
    
    ValidateEmployeeResponse validateEmployee(String email, String password);
}