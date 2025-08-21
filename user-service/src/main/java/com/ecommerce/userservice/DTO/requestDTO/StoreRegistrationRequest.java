package com.ecommerce.userservice.DTO.requestDTO;

import com.ecommerce.userservice.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StoreRegistrationRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username must be between 4-20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Be sure that the password has: minimum 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special character")
    private String password;

    @NotNull(message = "Role is required")
    private Role role = Role.STORE;

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 50, message = "Store name must be between 2-50 characters")
    private String storeName;

    @Size(max = 500, message = "Store description cannot exceed 500 characters")
    private String storeDescription;

    @NotBlank(message = "Document IDs are required")
    private String documentIDs;

    private String contactPersonName;
}
