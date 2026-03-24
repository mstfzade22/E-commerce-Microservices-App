package com.ecommerce.userservice.mapper;

import com.ecommerce.userservice.dto.request.RegistrationRequest;
import com.ecommerce.userservice.dto.response.LoginResponse;
import com.ecommerce.userservice.dto.response.RegistrationResponse;
import com.ecommerce.userservice.dto.response.UserProfileResponse;
import com.ecommerce.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {Instant.class}
)
public interface UserMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "locked", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", expression = "java(request.getRole() != null ? com.ecommerce.userservice.entity.Role.valueOf(request.getRole()) : null)")
    User toEntity(RegistrationRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "userType", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "message", constant = "User registered successfully")
    RegistrationResponse toRegistrationResponse(User user);

    @Mapping(target = "accessExpiresIn", ignore = true)
    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "message", constant = "Login successful")
    @Mapping(target = "sessionId", ignore = true)
    @Mapping(target = "loginAt", ignore = true)
    LoginResponse toLoginResponse(User user);

    @Mapping(target = "userType", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    UserProfileResponse toUserProfileResponse(User user);
}