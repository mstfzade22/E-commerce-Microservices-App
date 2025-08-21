package com.ecommerce.userservice.DTO.responseDTO;


import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRegistrationResponse {

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String userType;
    private String fullName;
    private boolean emailVerified;


}

