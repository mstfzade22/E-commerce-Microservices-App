package com.ecommerce.userservice.dto.response;

import lombok.*;

import java.time.Instant;


@Value
@Builder
public class TokenRefreshResponse {
     String sessionId;
     Instant refreshedAt;
     Long expiresIn;
     String message;
}