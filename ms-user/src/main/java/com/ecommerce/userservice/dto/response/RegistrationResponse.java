package com.ecommerce.userservice.dto.response;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class RegistrationResponse {
     UUID userId;
     String username;
     String email;
     String userType;
     Instant createdAt;
     Boolean emailVerified;
     String message;
}