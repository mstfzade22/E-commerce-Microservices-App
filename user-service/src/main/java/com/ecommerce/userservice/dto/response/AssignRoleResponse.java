package com.ecommerce.userservice.dto.response;

import com.ecommerce.userservice.entity.Role;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AssignRoleResponse {
    UUID userId;
    String username;
    Role previousRole;
    Role newRole;
    Instant assignedAt;
    String message;
}

