package com.ecommerce.userservice.DTO.responseDTO;


import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminRegistrationResponse{

    private String firstName;
    private String lastName;
    private Set<String> permissions;
    private String userType;
    private String fullName;
    private boolean emailVerified;

}
