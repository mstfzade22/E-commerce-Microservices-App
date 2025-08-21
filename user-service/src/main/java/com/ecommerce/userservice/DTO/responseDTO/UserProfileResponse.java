package com.ecommerce.userservice.DTO.responseDTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {    // user ozu oz profiline baxmaq isteyende

    private UUID uuid;
    private String username;
    private String email;
    private String userType;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

}

