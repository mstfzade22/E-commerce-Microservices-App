package com.ecommerce.notificationservice.dto;

import java.util.UUID;

public record UserInfo(
        UUID userId,
        String email,
        String firstName,
        String lastName
) {}
