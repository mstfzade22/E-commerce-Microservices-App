package com.ecommerce.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDenylistService {

    private final RedisTemplate<String, String> redisTemplate;

    public void denySession(String sessionId, long ttlSeconds) {
        try {
            String key = "denylist:session:" + sessionId;
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
            log.info("Session added to denylist: {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to add session to denylist: {}", sessionId, e);
            throw new RuntimeException("Failed to deny session", e);
        }
    }

    public boolean isSessionDenied(String sessionId) {
        try {
            String key = "denylist:session:" + sessionId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check session denylist: {}", sessionId, e);
            return false;
        }
    }

    public void removeSessionFromDenylist(String sessionId) {
        try {
            String key = "denylist:session:" + sessionId;
            redisTemplate.delete(key);
            log.info("Session removed from denylist: {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to remove session from denylist: {}", sessionId, e);
        }
    }
}