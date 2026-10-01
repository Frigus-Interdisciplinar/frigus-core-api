package com.frigus.coreapi.security;

import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {
    @Mock UserRepository users;
    TokenProvider tokens;
    WebSocketAuthInterceptor interceptor;
    User user;

    @BeforeEach
    void setUp() {
        tokens = new TokenProvider();
        ReflectionTestUtils.setField(tokens, "secret", "websocket-test-secret-with-32-characters");
        ReflectionTestUtils.setField(tokens, "expirationTime", 60_000L);
        interceptor = new WebSocketAuthInterceptor(tokens, users);
        user = User.builder().id(UUID.randomUUID()).role(Role.USER).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Authorization", "authorization", "token", "Token"})
    void authenticatesConnectWithEachSupportedTokenHeader(String header) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        String token = tokens.generateAccessToken(user);
        accessor.setNativeHeader(header, header.equalsIgnoreCase("authorization") ? "Bearer " + token : token);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        Message<byte[]> message = message(accessor);

        assertThat(interceptor.preSend(message, null)).isSameAs(message);

        Authentication authentication = (Authentication) accessor.getUser();
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid"})
    void missingOrInvalidCredentialsDoNotAuthenticateOrQueryUsers(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (!token.isEmpty()) accessor.setNativeHeader("Authorization", "Bearer " + token);
        interceptor.preSend(message(accessor), null);
        assertThat(accessor.getUser()).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void validTokenForDeletedUserDoesNotAuthenticate() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("token", tokens.generateAccessToken(user));
        when(users.findById(user.getId())).thenReturn(Optional.empty());
        interceptor.preSend(message(accessor), null);
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    void sendFrameCannotReplaceConnectionPrincipalWithTokenHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setNativeHeader("token", tokens.generateAccessToken(user));
        interceptor.preSend(message(accessor), null);
        assertThat(accessor.getUser()).isNull();
        verifyNoInteractions(users);
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
