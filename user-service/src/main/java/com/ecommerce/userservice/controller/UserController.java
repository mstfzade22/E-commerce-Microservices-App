package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.DTO.requestDTO.AssignRoleRequest;
import com.ecommerce.userservice.DTO.requestDTO.ChangePasswordRequest;
import com.ecommerce.userservice.DTO.responseDTO.UserRegistrationResponse;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController{

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserRegistrationResponse>> getAllUsers(){
        return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK);
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN') or #uuid == authentication.principal.id")
    public ResponseEntity<UserRegistrationResponse> getUserById(@PathVariable UUID uuid){
        return new ResponseEntity<>(userService.getUserById(uuid), HttpStatus.OK);
    }

    @PostMapping("/{uuid}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRegistrationResponse> assignRole(@PathVariable UUID uuid, @Valid @RequestBody AssignRoleRequest assignRoleRequest){
        return new ResponseEntity<>(userService.assignRole(uuid, assignRoleRequest), HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRegistrationResponse> removeRole(@PathVariable UUID uuid, @Valid @RequestBody AssignRoleRequest assignRoleRequest){
        return new ResponseEntity<>(userService.removeRole(uuid, assignRoleRequest), HttpStatus.OK);
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser (UUID uuid){
        userService.deleteUser(uuid);
        return ResponseEntity.ok("User deleted succefully");
    }

    @GetMapping("/me")
    public ResponseEntity<UserRegistrationResponse> getCurrentUser(@RequestParam UUID userId) {
        UserRegistrationResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me/password")
    public ResponseEntity<String> changeCurrentUserPassword(@RequestParam UUID userId, @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        userService.changePassword(userId, changePasswordRequest);
        return ResponseEntity.ok("Password changed successfully");
    }


}
