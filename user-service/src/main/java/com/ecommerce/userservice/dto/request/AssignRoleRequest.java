package com.ecommerce.userservice.dto.request;

import com.ecommerce.userservice.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AssignRoleRequest {
    @NotBlank(message = "Role cannot be blank")
    private Role role;
}

