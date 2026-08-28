package com.frigus.coreapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @InjectMocks private RefreshTokenService service;

    @Test
    void storesRefreshTokensForThirtyDaysAndCanValidateAndDeleteThem() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String token = service.createRefreshToken(userId);
        verify(valueOperations).set(eq("refresh:" + token), eq(userId.toString()), eq(30L), eq(TimeUnit.DAYS));

        when(valueOperations.get("refresh:known")).thenReturn(userId.toString());
        assertThat(service.validateAndGetUserId("known")).isEqualTo(userId);
        when(valueOperations.get("refresh:expired")).thenReturn(null);
        assertThat(service.validateAndGetUserId("expired")).isNull();
        service.deleteRefreshToken("known");
        verify(redisTemplate).delete("refresh:known");
    }
}
