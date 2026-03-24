package com.ecommerce.userservice.dto.response;

import lombok.*;

import java.time.Instant;

@Value
@Builder
public class ChangePasswordResponse {
    String message;
    Instant passwordChangedAt;
    boolean requiresReauthentication;
}
