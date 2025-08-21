package com.ecommerce.userservice.DTO.responseDTO;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoreRegistrationResponse {


    private String userType;
    private String businessName;
    private String storeStatus;
    private boolean emailVerified;

    private String storeName;
    private String storeDescription;
    private String contactPersonName;
    private boolean storeVerified;
    private LocalDateTime storeCreatedAt;

}
