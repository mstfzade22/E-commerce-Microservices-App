package com.ecommerce.userservice.DTO.requestDTO;

import com.ecommerce.userservice.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CustomerRegistrationRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username must be between 4-20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Be sure that the password has: minimum 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special character"
    )
    private String password;

    @NotNull(message = "Role is required")
    private Role role = Role.CUSTOMER;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
}
