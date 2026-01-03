package com.ecommerce.userservice.mapper;

import com.ecommerce.userservice.dto.response.ChangePasswordResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {Instant.class}
)
public interface PasswordMapper {
    default ChangePasswordResponse toChangePasswordResponse() {
        return ChangePasswordResponse.builder()
                .message("Password changed successfully")
                .passwordChangedAt(Instant.now())
                .build();
    }
}