package com.frigus.coreapi.security;

import com.frigus.coreapi.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenProviderTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"expired", "issuer", "signature"})
    void rejectsExpiredWrongIssuerAndWrongSignatureTokens(String scenario) {
        String secret = "test-signing-secret-with-at-least-32-characters";
        TokenProvider provider = new TokenProvider();
        ReflectionTestUtils.setField(provider, "secret", secret);
        String token = com.auth0.jwt.JWT.create()
                .withSubject(UUID.randomUUID().toString())
                .withIssuer(scenario.equals("issuer") ? "another-api" : "frigus")
                .withExpiresAt(java.time.Instant.now().plusSeconds(scenario.equals("expired") ? -60 : 600))
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256(scenario.equals("signature") ? "different-secret" : secret));

        assertThat(provider.validateAccessToken(token)).isNull();
        assertThat(provider.validateAccessTokenAndGetSubject(token)).isNull();
    }

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
