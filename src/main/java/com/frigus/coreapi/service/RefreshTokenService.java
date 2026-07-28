package com.frigus.coreapi.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final StringRedisTemplate redisTemplate;
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 30;

    public String createRefreshToken(UUID userId) {
        String refreshToken = UUID.randomUUID().toString();
        String key = "refresh:" + refreshToken;

        redisTemplate.opsForValue().set(key, userId.toString(), REFRESH_TOKEN_EXPIRATION_DAYS, TimeUnit.DAYS);
        return refreshToken;
    }

    public UUID validateAndGetUserId(String refreshToken) {
        String key = "refresh:" + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            return null;
        }

        return UUID.fromString(userId);
    }

    public void deleteRefreshToken(String refreshToken) {
        String key = "refresh:" + refreshToken;
        redisTemplate.delete(key);
    }
}
