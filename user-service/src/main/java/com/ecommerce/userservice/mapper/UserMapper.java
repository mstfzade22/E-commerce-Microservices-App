package com.ecommerce.userservice.mapper;

import com.ecommerce.userservice.DTO.requestDTO.AdminRegistrationRequest;
import com.ecommerce.userservice.DTO.requestDTO.CustomerRegistrationRequest;
import com.ecommerce.userservice.DTO.requestDTO.StoreRegistrationRequest;
import com.ecommerce.userservice.DTO.responseDTO.AdminRegistrationResponse;
import com.ecommerce.userservice.DTO.responseDTO.CustomerRegistrationResponse;
import com.ecommerce.userservice.DTO.responseDTO.StoreRegistrationResponse;
import com.ecommerce.userservice.DTO.responseDTO.UserRegistrationResponse;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "userType", expression = "java(formatRoles(user.getRoles()))")
    @Mapping(target = "emailVerified", source = "enabled")
    UserRegistrationResponse toUserRegistrationResponse(User user);

    @Mapping(target = "userType", expression = "java(formatRoles(user.getRoles()))")
    @Mapping(target = "emailVerified", source = "enabled")
    @Mapping(target = "fullName", source = "username")
    CustomerRegistrationResponse toCustomerRegistrationResponse(User user);

    @Mapping(target = "userType", expression = "java(formatRoles(user.getRoles()))")
    @Mapping(target = "emailVerified", source = "enabled")
    @Mapping(target = "businessName", source = "username")
    @Mapping(target = "storeStatus", constant = "PENDING_APPROVAL")
    StoreRegistrationResponse toStoreRegistrationResponse(User user);

    @Mapping(target = "userType", expression = "java(formatRoles(user.getRoles()))")
    @Mapping(target = "emailVerified", source = "enabled")
    @Mapping(target = "fullName", source = "username")
    @Mapping(target = "permissions", expression = "java(formatAdminPermissions())")
    AdminRegistrationResponse toAdminRegistrationResponse(User user);


    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "enabled", constant = "false")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    User toCustomer(CustomerRegistrationRequest request);

    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "enabled", constant = "false")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    User toStore(StoreRegistrationRequest request);

    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    User toAdmin(AdminRegistrationRequest request);

    default String formatRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        return roles.stream()
                .map(Role::name)
                .collect(Collectors.joining(", "));
    }

    default Set<String> formatAdminPermissions() {
        return Set.of("USER_MANAGEMENT", "STORE_APPROVAL", "SYSTEM_CONFIG", "REPORTS");
    }

    List<CustomerRegistrationResponse> toCustomerRegistrationResponseList(List<User> users);
    List<StoreRegistrationResponse> toStoreRegistrationResponseList(List<User> users);
    List<AdminRegistrationResponse> toAdminRegistrationResponseList(List<User> users);

    default List<UserRegistrationResponse> toRegistrationResponseList(List<User> users) {
        return users.stream()
                .map(this::toUserRegistrationResponse)
                .collect(Collectors.toList());
    }
}
