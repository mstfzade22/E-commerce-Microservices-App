package com.ecommerce.userservice.service;

import com.ecommerce.userservice.DTO.requestDTO.LoginRequest;
import com.ecommerce.userservice.DTO.requestDTO.TokenRefreshRequest;
import com.ecommerce.userservice.DTO.responseDTO.LoginResponse;
import com.ecommerce.userservice.DTO.responseDTO.TokenRefreshResponse;
import com.ecommerce.userservice.DTO.responseDTO.UserProfileResponse;
import com.ecommerce.userservice.entity.RefreshToken;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.TokenRefreshException;
import com.ecommerce.userservice.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public LoginResponse authenticate(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            User user = getUserByUsername(loginRequest.getUsername());

            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUuid());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs())
                    .user(UserProfileResponse.builder()
                            .uuid(user.getUuid())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .userType(user.getRoles().stream()
                                    .map(Role::name)
                                    .collect(Collectors.joining(", ")))
                            .build())
                    .build();


        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid username or password");
        } catch (DisabledException ex) {
            throw new DisabledException("Account is disabled");
        }
    }

    public void logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            User user = getUserByUsername(username);
            refreshTokenService.deleteByUser(user);
        }
    }

    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String refreshTokenValue = request.getRefreshToken();

        return refreshTokenService.findByToken(refreshTokenValue)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserUuid)
                .map(userUuid -> {
                    User user = getUserByUuid(userUuid);
                    String newAccessToken = jwtTokenProvider.generateAccessTokenFromUser(user);
                    return TokenRefreshResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(refreshTokenValue)
                            .tokenType("Bearer")
                            .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs())
                            .build();
                })
                .orElseThrow(() -> new TokenRefreshException(refreshTokenValue, "Refresh token is not in database!"));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private User getUserByUuid(UUID uuid) {
        return (User) userRepository.findByUuid(uuid).orElseThrow(() -> new UsernameNotFoundException("User not found by uuid: " + uuid));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
