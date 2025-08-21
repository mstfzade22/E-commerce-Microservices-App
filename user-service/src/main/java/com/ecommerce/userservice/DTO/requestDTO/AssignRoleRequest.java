package com.ecommerce.userservice.DTO.requestDTO;

import com.ecommerce.userservice.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AssignRoleRequest {
    @NotBlank(message = "Role cannot be blank")
    @Pattern(regexp = "^(USER|CUSTOMER|STORE|ADMIN)$", message = "Invalid role")
    private Role role;
}


