package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.request.ChangePasswordRequest;
import com.ecommerce.userservice.dto.response.ChangePasswordResponse;
import com.ecommerce.userservice.dto.response.UserProfileResponse;
import com.ecommerce.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "cookieAuth")
@Tag(name = "User Management", description = "User profile and account management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile information")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        // userDetails.getUsername() contains the userId (UUID) from JWT token
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.debug("Fetching profile for user ID: {}", userId);
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.principal.username")
    @Operation(summary = "Get user by ID", description = "Returns user profile by ID (Admin or self only)")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID userId) {
        log.debug("Fetching user profile by ID: {}", userId);
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates the authenticated user's profile information")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> updates) {

        // userDetails.getUsername() contains the userId (UUID) from JWT token
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Updating profile for user ID: {}", userId);

        String firstName = updates.get("firstName");
        String lastName = updates.get("lastName");
        String email = updates.get("email");

        UserProfileResponse updated = userService.updateUserProfile(userId, firstName, lastName, email);

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/password")
    @Operation(summary = "Change password", description = "Changes the authenticated user's password and revokes all sessions")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request)
    {
        // userDetails.getUsername() contains the userId (UUID) from JWT token
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("Password change request for user ID: {}", userId);

        ChangePasswordResponse response = userService.changePassword(
                userId,
                request.getCurrentPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lock user account", description = "Locks a user account and revokes all sessions (Admin only)")
    public ResponseEntity<Map<String, String>> lockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails adminDetails) {

        log.warn("Admin {} locking user account: {}", adminDetails.getUsername(), userId);
        userService.lockUser(userId, adminDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User account locked successfully",
                "userId", userId.toString(),
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock user account", description = "Unlocks a previously locked user account (Admin only)")
    public ResponseEntity<Map<String, String>> unlockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails adminDetails) {

        log.info("Admin {} unlocking user account: {}", adminDetails.getUsername(), userId);
        userService.unlockUser(userId, adminDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User account unlocked successfully",
                "userId", userId.toString(),
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @PostMapping("/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable user account", description = "Disables a user account and revokes all sessions (Admin only)")
    public ResponseEntity<Map<String, String>> disableUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails adminDetails) {

        log.warn("Admin {} disabling user account: {}", adminDetails.getUsername(), userId);
        userService.disableUser(userId, adminDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User account disabled successfully",
                "userId", userId.toString(),
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @PostMapping("/{userId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable user account", description = "Enables a previously disabled user account (Admin only)")
    public ResponseEntity<Map<String, String>> enableUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails adminDetails) {

        log.info("Admin {} enabling user account: {}", adminDetails.getUsername(), userId);
        userService.enableUser(userId, adminDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User account enabled successfully",
                "userId", userId.toString(),
                "timestamp", java.time.Instant.now().toString()
        ));
    }


}
