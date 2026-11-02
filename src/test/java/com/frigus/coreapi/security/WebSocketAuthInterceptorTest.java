package com.frigus.coreapi.security;

import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.repository.ConversationRepository;
import com.frigus.coreapi.repository.ConversationParticipantRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {
    @Mock UserRepository users;
    @Mock UserGroupRepository groups;
    @Mock ConversationRepository conversations;
    @Mock ConversationParticipantRepository participants;
    TokenProvider tokens;
    WebSocketAuthInterceptor interceptor;
    User user;

    @BeforeEach
    void setUp() {
        tokens = new TokenProvider();
        ReflectionTestUtils.setField(tokens, "secret", "websocket-test-secret-with-32-characters");
        ReflectionTestUtils.setField(tokens, "expirationTime", 60_000L);
        interceptor = new WebSocketAuthInterceptor(tokens, users, groups, conversations, participants);
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
    void missingOrInvalidCredentialsAreRejected(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (!token.isEmpty()) accessor.setNativeHeader("Authorization", "Bearer " + token);
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(accessor.getUser()).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void validTokenForDeletedUserDoesNotAuthenticate() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("token", tokens.generateAccessToken(user));
        when(users.findById(user.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    void sendFrameCannotReplaceConnectionPrincipalWithTokenHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setNativeHeader("token", tokens.generateAccessToken(user));
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(accessor.getUser()).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void subscribeRequiresCurrentMembershipAndKnownTopic() {
        UUID groupId = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null));
        accessor.setDestination("/topic/groups." + groupId + ".messages");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(groups.existsByUserIdAndGroupId(user.getId(), groupId)).thenReturn(true, false);

        assertThat(interceptor.preSend(message(accessor), null)).isNotNull();
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        accessor.setDestination("/topic/other");
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void conversationSubscriptionRequiresActiveParticipant() {
        UUID id = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null));
        accessor.setDestination("/topic/conversations." + id);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(conversations.findById(id)).thenReturn(Optional.of(Conversation.builder().id(id).build()));
        when(participants.isUserActiveParticipant(id, user.getId())).thenReturn(true, false);

        assertThat(interceptor.preSend(message(accessor), null)).isNotNull();
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
