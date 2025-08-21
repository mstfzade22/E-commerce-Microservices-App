package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.DTO.requestDTO.*;
import com.ecommerce.userservice.DTO.responseDTO.LoginResponse;
import com.ecommerce.userservice.DTO.responseDTO.MessageResponse;
import com.ecommerce.userservice.DTO.responseDTO.TokenRefreshResponse;
import com.ecommerce.userservice.DTO.responseDTO.UserRegistrationResponse;
import com.ecommerce.userservice.service.AuthService;
import com.ecommerce.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;


    @PostMapping("/register/customer")
    public ResponseEntity<UserRegistrationResponse> registerCustomer(@Valid @RequestBody CustomerRegistrationRequest request) {
        UserRegistrationResponse response = userService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/store")
    public ResponseEntity<UserRegistrationResponse> registerStore(@Valid @RequestBody StoreRegistrationRequest request) {
        UserRegistrationResponse response = userService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRegistrationResponse> registerAdmin(@Valid @RequestBody AdminRegistrationRequest request) {
        UserRegistrationResponse response = userService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

}