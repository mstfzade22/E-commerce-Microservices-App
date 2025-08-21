package com.ecommerce.userservice.service;

import com.ecommerce.userservice.entity.RefreshToken;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.TokenRefreshException;
import com.ecommerce.userservice.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private Long refreshTokenDurationMs;

    public RefreshToken createRefreshToken(UUID userUuid) {
        deleteByUserUuid(userUuid);

        RefreshToken refreshToken = RefreshToken.builder()
                .userUuid(userUuid)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUserUuid(user.getUuid());
    }

    public void deleteByUserUuid(UUID userUuid) {
        refreshTokenRepository.deleteByUserUuid(userUuid);
    }
}
