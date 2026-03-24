package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegistrationRequest;
import com.ecommerce.userservice.dto.response.LoginResponse;
import com.ecommerce.userservice.dto.response.RegistrationResponse;
import com.ecommerce.userservice.dto.response.TokenRefreshResponse;
import com.ecommerce.userservice.entity.RefreshToken;
import com.ecommerce.userservice.entity.Session;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.AuthenticationException;
import com.ecommerce.userservice.exception.DuplicateUserException;
import com.ecommerce.userservice.exception.InvalidTokenException;
import com.ecommerce.userservice.exception.TokenReusedException;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.RefreshTokenRepository;
import com.ecommerce.userservice.repository.SessionRepository;
import com.ecommerce.userservice.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final RedisDenylistService denylistService;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        log.info("Registration request received for username: {}", request.getUsername());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already in use");
        }

        User user = userMapper.toEntity(request);
        user.setUserId(UUID.randomUUID());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(Instant.now());

        userRepository.save(user);
        log.info("Registered new user: {} (id={})", user.getUsername(), user.getUserId());

        return userMapper.toRegistrationResponse(user);
    }

    @Transactional
    public AuthResult<LoginResponse> login(LoginRequest request, String ipAddress, String deviceInfo) {
        log.info("Login request received for username: {}", request.getUsername());

        Optional<User> maybeUser = userRepository.findByUsername(request.getUsername());
        if (maybeUser.isEmpty()) {
            log.warn("Login attempt with unknown identifier: {}", request.getUsername());
            throw new AuthenticationException("Invalid credentials");
        }

        User user = maybeUser.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", user.getUserId());
            throw new AuthenticationException("Invalid credentials");
        }

        String role = user.getRole().name();
        TokenPair tokens = createSessionForUser(user.getUserId(), role, ipAddress, deviceInfo);

        log.info("User {} logged in successfully (username: {})", user.getUserId(), request.getUsername());

        LoginResponse response = userMapper.toLoginResponse(user);

        response.setSessionId(tokens.sessionId);
        response.setLoginAt(Instant.now());
        response.setAccessExpiresIn(jwtUtil.getAccessTokenExpiration());

        return new AuthResult<>(response, tokens);
    }

    @Transactional
    public TokenPair createSessionForUser(UUID userId, String role, String ipAddress, String deviceInfo) {
        String sessionId = "session_" + UUID.randomUUID();

        JwtUtil.TokenData accessData = jwtUtil.generateAccessToken(userId, sessionId, role);
        JwtUtil.TokenData refreshData = jwtUtil.generateRefreshToken(userId, sessionId);

        log.debug("Generated tokens - Access JTI: {}, Refresh JTI: {}, Session: {}, Role: {}",
                accessData.jti(), refreshData.jti(), sessionId, role);

        Instant now = Instant.now();
        Instant refreshExpiry = now.plusMillis(jwtUtil.getRefreshTokenExpirationMs());

        Session session = Session.builder()
                .sessionId(sessionId)
                .userId(userId)
                .currentRefreshJti(refreshData.jti())
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .createdAt(now)
                .lastActivityAt(now)
                .expiresAt(refreshExpiry)
                .isActive(true)
                .build();

        sessionRepository.save(session);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .jti(refreshData.jti())
                .sessionId(sessionId)
                .userId(userId)
                .tokenHash(hashToken(refreshData.token()))
                .createdAt(now)
                .expiresAt(refreshExpiry)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        log.debug("Persisted session {} and refresh token {} for user {}", sessionId, refreshData.jti(), userId);
        return new TokenPair(accessData.token(), refreshData.token(), sessionId);
    }

    @Transactional
    public void logout(String refreshToken) {
        try {
            if (refreshToken == null) {
                log.warn("Logout called without refresh token");
                return;
            }

            Claims refreshClaims = jwtUtil.validateRefreshToken(refreshToken);
            String sessionId = refreshClaims.get("sid", String.class);
            String refreshJti = refreshClaims.getId();

            long sessionTTL = jwtUtil.getRefreshTokenExpirationMs() / 1000;
            denylistService.denySession(sessionId, sessionTTL);

            sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                session.setIsActive(false);
                session.setRevokedAt(Instant.now());
                sessionRepository.save(session);
            });

            refreshTokenRepository.findByJti(refreshJti).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
            });

            log.info("User logged out successfully. Session: {}", sessionId);

        } catch (Exception e) {
            log.error("Error during logout", e);
        }
    }

    @Transactional
    public AuthResult<TokenRefreshResponse> refresh(String oldRefreshToken, String ipAddress, String deviceInfo) {
        if (oldRefreshToken == null) {
            throw new InvalidTokenException("Refresh token not found");
        }

        Claims claims;
        try {
            claims = jwtUtil.validateRefreshToken(oldRefreshToken);
        } catch (Exception e) {
            log.error("Invalid refresh token", e);
            throw new InvalidTokenException("Invalid refresh token");
        }

        String providedRefreshJti = claims.getId();
        String sessionId = claims.get("sid", String.class);
        UUID userId = UUID.fromString(claims.getSubject());

        log.debug("Refresh request - Session: {}, Provided JTI: {}", sessionId, providedRefreshJti);

        if (denylistService.isSessionDenied(sessionId)) {
            log.warn("Refresh attempt with revoked session: {}", sessionId);
            throw new InvalidTokenException("Session revoked");
        }

        RefreshToken storedToken = refreshTokenRepository
                .findByJtiWithLock(providedRefreshJti)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (!passwordEncoder.matches(oldRefreshToken, storedToken.getTokenHash())) {
            log.error("Refresh token string does not match DB hash. Possible token theft or tampering. JTI: {}", providedRefreshJti);
            handleTokenReuse(sessionId, userId);
            throw new AuthenticationException("Invalid refresh token");
        }

        if (storedToken.getRevokedAt() != null || storedToken.isRevoked()) {
            log.warn("Refresh attempt with revoked token. JTI: {}", providedRefreshJti);
            throw new InvalidTokenException("Refesh token revoked");
        }

        if (storedToken.getUsedAt() != null) {
            log.error("TOKEN REUSE DETECTED! Session: {}, User: {}, JTI: {}",
                    sessionId, userId, providedRefreshJti);
            handleTokenReuse(sessionId, userId);
            throw new TokenReusedException("Token reuse detected. Session revoked for security.");
        }

        if (!storedToken.getSessionId().equals(sessionId)) {
            log.error("Session mismatch! Token session: {}, Claim session: {}",
                    storedToken.getSessionId(), sessionId);
            throw new InvalidTokenException("Session mismatch");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        String role = user.getRole().name();

        JwtUtil.TokenData newAccessData = jwtUtil.generateAccessToken(userId, sessionId, role);
        JwtUtil.TokenData newRefreshData = jwtUtil.generateRefreshToken(userId, sessionId);

        log.debug("Rotating tokens - Old refresh JTI: {}, New refresh JTI: {}, New access JTI: {}",
                providedRefreshJti, newRefreshData.jti(), newAccessData.jti());

        storedToken.setUsedAt(Instant.now());
        storedToken.setReplacedBy(newRefreshData.jti());
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        Instant now = Instant.now();
        Instant refreshExpiry = now.plusMillis(jwtUtil.getRefreshTokenExpirationMs());
        RefreshToken newToken = RefreshToken.builder()
                .jti(newRefreshData.jti())
                .sessionId(sessionId)
                .userId(userId)
                .tokenHash(hashToken(newRefreshData.token()))
                .previousJti(providedRefreshJti)
                .createdAt(now)
                .expiresAt(refreshExpiry)
                .revoked(false)
                .build();

        refreshTokenRepository.save(newToken);

        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setLastActivityAt(Instant.now());
            session.setCurrentRefreshJti(newRefreshData.jti());
            sessionRepository.save(session);
        });

        log.info("Token refresh successful for user {} in session {}", userId, sessionId);

        TokenRefreshResponse response = TokenRefreshResponse.builder()
                .message("Tokens refreshed successfully")
                .sessionId(sessionId)
                .refreshedAt(Instant.now())
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();

        return new AuthResult<>(response, new TokenPair(newAccessData.token(), newRefreshData.token(), sessionId));
    }

    private void handleTokenReuse(String sessionId, UUID userId) {
        log.warn("SECURITY ALERT: Token reuse detected! Session: {}, User: {}", sessionId, userId);

        long sessionTTL = jwtUtil.getRefreshTokenExpirationMs() / 1000;
        denylistService.denySession(sessionId, sessionTTL);

        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setIsActive(false);
            session.setRevokedAt(Instant.now());
            sessionRepository.save(session);
        });

        try {
            List<RefreshToken> tokens = refreshTokenRepository.findAllBySessionId(sessionId);
            for (RefreshToken t : tokens) {
                t.revoke();
            }
            refreshTokenRepository.saveAll(tokens);
        } catch (Exception ex) {
            log.error("Failed to revoke tokens for session {}: {}", sessionId, ex.getMessage(), ex);
        }
    }

    private String hashToken(String token) {
        return passwordEncoder.encode(token);
    }

    public static class TokenPair {
        public final String accessToken;
        public final String refreshToken;
        public final String sessionId;

        public TokenPair(String accessToken, String refreshToken, String sessionId) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.sessionId = sessionId;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class AuthResult<T> {
        private final T response;
        private final TokenPair tokens;
    }
}

