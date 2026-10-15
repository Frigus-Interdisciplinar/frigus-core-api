package com.frigus.coreapi.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.repository.ConversationParticipantRepository;
import com.frigus.coreapi.repository.ConversationRepository;
import com.frigus.coreapi.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private static final Pattern CONVERSATION_TOPIC = Pattern.compile("^/topic/conversations\\.([0-9a-fA-F-]{36})$");
    private static final Pattern GROUP_TOPIC = Pattern.compile("^/topic/groups\\.([0-9a-fA-F-]{36})(?:\\.messages)?$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = recoverToken(accessor);

            if (token != null) {
                DecodedJWT decodedJWT = tokenProvider.validateAccessToken(token);
                if (decodedJWT != null) {
                    User user = null;
                    try {
                        user = userRepository.findById(UUID.fromString(decodedJWT.getSubject())).orElse(null);
                    } catch (IllegalArgumentException ignored) {
                        // Invalid subject is an unauthenticated connection.
                    }

                    if (user != null) {
                        var authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                        );
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                        accessor.setUser(authentication);
                        log.debug("WebSocket authenticated user: {}", user.getEmail());
                    }
                }
            }
            if (accessor.getUser() == null) {
                throw new org.springframework.security.access.AccessDeniedException("WebSocket authentication required");
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                || StompCommand.SEND.equals(accessor.getCommand())) {
            User user = authenticatedUser(accessor);
            String destination = accessor.getDestination();
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                authorizeSubscription(user, destination);
            } else if (!"/app/chat.send".equals(destination) && !"/app/groups.send".equals(destination)) {
                throw new org.springframework.security.access.AccessDeniedException("Destination not allowed");
            }
        }

        return message;
    }

    private User authenticatedUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof org.springframework.security.core.Authentication authentication
                && authentication.getPrincipal() instanceof User user) {
            User current = userRepository.findById(user.getId()).orElse(null);
            if (current != null) {
                accessor.setUser(new UsernamePasswordAuthenticationToken(current, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + current.getRole().name()))));
                return current;
            }
        }
        throw new org.springframework.security.access.AccessDeniedException("WebSocket authentication required");
    }

    private void authorizeSubscription(User user, String destination) {
        Matcher conversation = CONVERSATION_TOPIC.matcher(destination == null ? "" : destination);
        if (conversation.matches()) {
            UUID id = UUID.fromString(conversation.group(1));
            var entity = conversationRepository.findById(id)
                    .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Conversation not found"));
            if (!participantRepository.isUserActiveParticipant(id, user.getId())
                    || (entity.getGroup() != null && !hasGroupAccess(user, entity.getGroup().getId()))) {
                throw new org.springframework.security.access.AccessDeniedException("Conversation access denied");
            }
            return;
        }
        Matcher group = GROUP_TOPIC.matcher(destination == null ? "" : destination);
        if (group.matches() && hasGroupAccess(user, UUID.fromString(group.group(1)))) {
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException("Topic access denied");
    }

    private boolean hasGroupAccess(User user, UUID groupId) {
        return user.getRole() == Role.ADMIN || userGroupRepository.existsByUserIdAndGroupId(user.getId(), groupId);
    }

    private String recoverToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            authHeaders = accessor.getNativeHeader("authorization");
        }
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return authHeader;
        }

        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders == null || tokenHeaders.isEmpty()) {
            tokenHeaders = accessor.getNativeHeader("Token");
        }
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }
}
