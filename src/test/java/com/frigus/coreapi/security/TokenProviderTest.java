package com.frigus.coreapi.security;

import com.frigus.coreapi.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderTest {
    @Test
    void generatesAndValidatesTokensAndRejectsInvalidValues() {
        TokenProvider provider = new TokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "a-secret-long-enough-for-hmac-validation");
        ReflectionTestUtils.setField(provider, "expirationTime", 60_000L);
        User user = User.builder().id(UUID.randomUUID()).build();

        String token = provider.generateAccessToken(user);
        assertThat(provider.validateAccessTokenAndGetSubject(token)).isEqualTo(user.getId().toString());
        assertThat(provider.validateAccessToken(token).getClaim("plan").asString()).isEqualTo("FREE");
        assertThat(provider.validateAccessTokenAndGetSubject("invalid")).isNull();
        assertThat(provider.validateAccessToken("invalid")).isNull();
    }
}
