package com.ecommerce.notificationservice.client;

import com.ecommerce.notificationservice.config.RedisConfig;
import com.ecommerce.notificationservice.dto.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${services.user-service.url}") String userServiceUrl) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    @Cacheable(value = RedisConfig.CacheNames.USER_INFO, key = "#userId", unless = "#result == null")
    public Optional<UserInfo> getUserById(UUID userId) {
        try {
            log.debug("Fetching user info for userId: {}", userId);
            UserInfo user = restClient.get()
                    .uri("/internal/users/{id}", userId)
                    .retrieve()
                    .body(UserInfo.class);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("Failed to fetch user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}
