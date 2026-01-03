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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

        if (request.getUsername() == null || request.getUsername().isBlank())
            throw new IllegalArgumentException("username required");
        if (request.getEmail() == null || request.getEmail().isBlank())
            throw new IllegalArgumentException("email required");
        if (request.getPassword() == null || request.getPassword().length() < 8)
            throw new IllegalArgumentException("password must be at least 8 characters");

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
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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
        TokenPair tokens = createSessionForUser(user.getUserId(), role, httpRequest);

        setAuthCookies(httpResponse, tokens);

        log.info("User {} logged in successfully (username: {})", user.getUserId(), request.getUsername());

        LoginResponse response = userMapper.toLoginResponse(user);

        response.setSessionId(tokens.sessionId);
        response.setLoginAt(Instant.now());
        response.setAccessExpiresIn(jwtUtil.getAccessTokenExpiration());

        return response;
    }

    @Transactional
    public TokenPair createSessionForUser(UUID userId, String role, HttpServletRequest request) {
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
                .deviceInfo(request == null ? null : request.getHeader("User-Agent"))
                .ipAddress(request == null ? null : getClientIp(request))
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
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = extractTokenFromCookie(request, "refresh_token");

            if (refreshToken == null) {
                log.warn("Logout called without refresh token");
                clearAuthCookies(response);
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
        } finally {
            clearAuthCookies(response);
        }
    }

    @Transactional
    public TokenRefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String oldRefreshToken = extractTokenFromCookie(request, "refresh_token");

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

        // Fetch user's role from database
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

        setAuthCookies(response, new TokenPair(newAccessData.token(), newRefreshData.token(), sessionId));

        log.info("Token refresh successful for user {} in session {}", userId, sessionId);

        return TokenRefreshResponse.builder()
                .message("Tokens refreshed successfully")
                .sessionId(sessionId)
                .refreshedAt(Instant.now())
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
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

    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setMaxAge(0);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setMaxAge(0);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        response.addCookie(refreshCookie);
    }

    private void setAuthCookies(HttpServletResponse response, TokenPair tokens) {
        Cookie accessCookie = new Cookie("access_token", tokens.accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true); // HTTPS - production-da true ele
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtUtil.getAccessTokenExpirationMs() / 1000));
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", tokens.refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true); // HTTPS - production-da true ele
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (jwtUtil.getRefreshTokenExpirationMs() / 1000));
        response.addCookie(refreshCookie);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
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
}