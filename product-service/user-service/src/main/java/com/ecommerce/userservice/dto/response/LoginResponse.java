package com.ecommerce.userservice.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String sessionId;
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private Long accessExpiresIn;
    private Instant loginAt;
    private String message;
}