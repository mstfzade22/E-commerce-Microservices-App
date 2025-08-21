package com.ecommerce.userservice.DTO.responseDTO;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserProfileResponse user;
}