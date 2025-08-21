package com.ecommerce.userservice.DTO.responseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationResponse {
    private UUID uuid;
    private String username;
    private String email;
    private String userType;
    private LocalDateTime createdAt;
    private Boolean emailVerified;
}
