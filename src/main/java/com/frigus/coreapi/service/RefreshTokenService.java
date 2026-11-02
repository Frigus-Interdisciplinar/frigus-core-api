package com.frigus.coreapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 30;
    private static final Duration SESSION_TTL = Duration.ofDays(REFRESH_TOKEN_EXPIRATION_DAYS);
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String USER_SESSIONS_PREFIX = "refresh-sessions:";
    private static final String SESSION_PREFIX = "session:";
    private static final String SESSION_TOUCH_PREFIX = "session-touch:";

    private final StringRedisTemplate redisTemplate;

    public String createRefreshToken(UUID userId) {
        return createRefreshToken(userId, 0);
    }

    public String createRefreshToken(UUID userId, int version) {
        return createRefreshToken(userId, version, UUID.randomUUID(), "");
    }

    public String createRefreshToken(UUID userId, int version, UUID sessionId, String userAgent) {
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                refreshKey(refreshToken),
                userId + ":" + version + ":" + sessionId,
                REFRESH_TOKEN_EXPIRATION_DAYS,
                TimeUnit.DAYS);

        String sessionKey = sessionKey(sessionId);
        Object existingUserId = redisTemplate.opsForHash().get(sessionKey, "userId");
        if (existingUserId == null) {
            redisTemplate.opsForHash().putAll(sessionKey, Map.of(
                    "userId", userId.toString(),
                    "userAgent", normalizeUserAgent(userAgent),
                    "createdAt", Instant.now().toString(),
                    "lastActiveAt", Instant.now().toString()));
        } else {
            redisTemplate.opsForHash().put(sessionKey, "lastActiveAt", Instant.now().toString());
            if (userAgent != null && !userAgent.isBlank()) {
                redisTemplate.opsForHash().put(sessionKey, "userAgent", normalizeUserAgent(userAgent));
            }
        }
        redisTemplate.opsForHash().put(sessionKey, "refreshToken", refreshToken);
        redisTemplate.expire(sessionKey, SESSION_TTL);
        redisTemplate.opsForSet().add(userSessionsKey(userId), sessionId.toString());
        redisTemplate.expire(userSessionsKey(userId), SESSION_TTL);
        return refreshToken;
    }

    public UUID validateAndGetUserId(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return null;
        String value = redisTemplate.opsForValue().get(refreshKey(refreshToken));
        if (value == null) return null;
        try {
            return UUID.fromString(value.split(":", -1)[0]);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** Returns the session for a refresh token, adding metadata to tokens issued before session tracking. */
    public UUID ensureSessionId(String refreshToken, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) return null;
        String key = refreshKey(refreshToken);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return null;
        String[] parts = value.split(":", -1);
        if (parts.length >= 3) {
            try {
                UUID sessionId = UUID.fromString(parts[2]);
                if (!Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(sessionId)))) {
                    registerLegacySession(UUID.fromString(parts[0]), sessionId, refreshToken, userAgent);
                }
                return sessionId;
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
        if (parts.length != 2) return null;
        try {
            UUID userId = UUID.fromString(parts[0]);
            UUID sessionId = UUID.randomUUID();
            long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            long ttl = ttlSeconds > 0 ? ttlSeconds : SESSION_TTL.toSeconds();
            redisTemplate.opsForValue().set(key, value + ":" + sessionId, ttl, TimeUnit.SECONDS);
            registerLegacySession(userId, sessionId, refreshToken, userAgent);
            return sessionId;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public String getSessionUserAgent(UUID sessionId) {
        if (sessionId == null) return "";
        Object value = redisTemplate.opsForHash().get(sessionKey(sessionId), "userAgent");
        return value == null ? "" : value.toString();
    }

    /** Rotates the refresh token but leaves the same device session active. */
    public String rotateRefreshToken(String oldToken, UUID userId, int version, UUID sessionId) {
        String sessionKey = sessionKey(sessionId);
        String userAgent = getSessionUserAgent(sessionId);
        redisTemplate.delete(refreshKey(oldToken));
        return createRefreshToken(userId, version, sessionId, userAgent);
    }

    /** Logout with a refresh token revokes the entire device session. */
    public void deleteRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String key = refreshKey(refreshToken);
        String value = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (value == null) return;
        String[] parts = value.split(":", -1);
        if (parts.length < 3) return;
        try {
            UUID userId = UUID.fromString(parts[0]);
            UUID sessionId = UUID.fromString(parts[2]);
            redisTemplate.delete(sessionKey(sessionId));
            redisTemplate.opsForSet().remove(userSessionsKey(userId), sessionId.toString());
            redisTemplate.delete(SESSION_TOUCH_PREFIX + sessionId);
        } catch (IllegalArgumentException ignored) {
            // A malformed legacy value can be deleted but cannot point to a session record.
        }
    }

    public boolean isCurrentVersion(String token, int version) {
        if (token == null || token.isBlank()) return false;
        String value = redisTemplate.opsForValue().get(refreshKey(token));
        if (value == null) return false;
        String[] parts = value.split(":", -1);
        try {
            return (parts.length < 2 ? 0 : Integer.parseInt(parts[1])) == version;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public boolean isSessionActive(UUID sessionId) {
        return sessionId != null && Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(sessionId)));
    }

    /** Updates last-active at most once every five minutes while still checking revocation per request. */
    public boolean touchSession(UUID sessionId) {
        if (!isSessionActive(sessionId)) return false;
        String touchKey = SESSION_TOUCH_PREFIX + sessionId;
        Boolean shouldTouch = redisTemplate.opsForValue().setIfAbsent(touchKey, "1", Duration.ofMinutes(5));
        if (Boolean.TRUE.equals(shouldTouch)) {
            redisTemplate.opsForHash().put(sessionKey(sessionId), "lastActiveAt", Instant.now().toString());
        }
        return true;
    }

    public List<DeviceSessionDto> listSessions(UUID userId, UUID currentSessionId) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) return List.of();
        List<DeviceSessionDto> sessions = new ArrayList<>();
        for (String rawSessionId : sessionIds) {
            try {
                UUID sessionId = UUID.fromString(rawSessionId);
                Map<Object, Object> metadata = redisTemplate.opsForHash().entries(sessionKey(sessionId));
                if (metadata.isEmpty() || !userId.toString().equals(metadata.get("userId"))) {
                    redisTemplate.opsForSet().remove(userSessionsKey(userId), rawSessionId);
                    continue;
                }
                String userAgent = String.valueOf(metadata.getOrDefault("userAgent", ""));
                sessions.add(new DeviceSessionDto(
                        sessionId,
                        deviceName(userAgent),
                        parseInstant(metadata.get("createdAt")),
                        parseInstant(metadata.get("lastActiveAt")),
                        sessionId.equals(currentSessionId),
                        null));
            } catch (IllegalArgumentException ignored) {
                redisTemplate.opsForSet().remove(userSessionsKey(userId), rawSessionId);
            }
        }
        sessions.sort((left, right) -> right.lastActiveAt().compareTo(left.lastActiveAt()));
        return List.copyOf(sessions);
    }

    public boolean revokeSession(UUID userId, UUID sessionId) {
        String sessionKey = sessionKey(sessionId);
        Object sessionUserId = redisTemplate.opsForHash().get(sessionKey, "userId");
        if (sessionUserId == null || !userId.toString().equals(sessionUserId.toString())) return false;
        Object token = redisTemplate.opsForHash().get(sessionKey, "refreshToken");
        if (token != null) redisTemplate.delete(refreshKey(token.toString()));
        redisTemplate.delete(sessionKey);
        redisTemplate.delete(SESSION_TOUCH_PREFIX + sessionId);
        redisTemplate.opsForSet().remove(userSessionsKey(userId), sessionId.toString());
        return true;
    }

    private void registerLegacySession(UUID userId, UUID sessionId, String refreshToken, String userAgent) {
        String now = Instant.now().toString();
        redisTemplate.opsForHash().putAll(sessionKey(sessionId), Map.of(
                "userId", userId.toString(),
                "refreshToken", refreshToken,
                "userAgent", normalizeUserAgent(userAgent),
                "createdAt", now,
                "lastActiveAt", now));
        redisTemplate.expire(sessionKey(sessionId), SESSION_TTL);
        redisTemplate.opsForSet().add(userSessionsKey(userId), sessionId.toString());
        redisTemplate.expire(userSessionsKey(userId), SESSION_TTL);
    }

    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "";
        return userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent;
    }

    private String deviceName(String userAgent) {
        String value = userAgent.toLowerCase();
        if (value.contains("ipad")) return "iPad";
        if (value.contains("iphone")) return "iPhone";
        if (value.contains("android")) return "Android";
        if (value.contains("windows")) return "Windows PC";
        if (value.contains("macintosh") || value.contains("mac os")) return "Mac";
        return userAgent.isBlank() ? "Dispositivo" : "Navegador";
    }

    private Instant parseInstant(Object value) {
        if (value == null) return Instant.EPOCH;
        try {
            return Instant.parse(value.toString());
        } catch (RuntimeException exception) {
            return Instant.EPOCH;
        }
    }

    private String refreshKey(String token) {
        return REFRESH_PREFIX + token;
    }

    private String sessionKey(UUID sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String userSessionsKey(UUID userId) {
        return USER_SESSIONS_PREFIX + userId;
    }

    public record DeviceSessionDto(
            UUID id,
            String deviceName,
            Instant createdAt,
            Instant lastActiveAt,
            boolean current,
            String location) {}
}
