package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.message.MessageResponseDto;
import com.frigus.coreapi.dto.message.MessageSendDto;
import com.frigus.coreapi.enums.MessageType;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.exception.UnauthorizedException;
import com.frigus.coreapi.mapper.MessageMapper;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.model.ConversationParticipant;
import com.frigus.coreapi.model.Message;
import com.frigus.coreapi.model.MessageRead;
import com.frigus.coreapi.model.MessageReadId;
import com.frigus.coreapi.model.ShoppingListProduct;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.ConversationParticipantRepository;
import com.frigus.coreapi.repository.ConversationRepository;
import com.frigus.coreapi.repository.MessageReadRepository;
import com.frigus.coreapi.repository.MessageRepository;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import com.frigus.coreapi.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageReadRepository messageReadRepository;
    private final ShoppingListProductRepository shoppingListProductRepository;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public Page<MessageResponseDto> getMessagesHistory(UUID conversationId, Pageable pageable) {
        User currentUser = requireCurrentUser();
        validateActiveParticipant(conversationId, currentUser.getId());

        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(messageMapper::toDto);
    }

    @Transactional
    public MessageResponseDto sendMessage(User sender, MessageSendDto dto) {
        User actualSender = sender != null ? sender : requireCurrentUser();
        UUID conversationId = dto.getConversationId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada", "Conversa com ID " + conversationId + " não existe"));

        ConversationParticipant cp = conversationParticipantRepository
                .findByIdConversationIdAndIdUserId(conversationId, actualSender.getId())
                .filter(p -> p.getLeftAt() == null)
                .orElseThrow(() -> new ForbiddenException("Acesso negado", "Você não é participante ativo desta conversa"));

        ShoppingListProduct shoppingListProduct = null;
        if (dto.getRelatedShoppingListProductId() != null) {
            shoppingListProduct = shoppingListProductRepository.findById(dto.getRelatedShoppingListProductId()).orElse(null);
        }

        MessageType type = dto.getMessageType() != null ? dto.getMessageType() : MessageType.TEXT;

        Message message = Message.builder()
                .conversationParticipants(cp)
                .messageType(type)
                .content(dto.getContent())
                .relatedShoppingListProduct(shoppingListProduct)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        message = messageRepository.save(message);

        // Update conversation timestamp
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        MessageResponseDto responseDto = messageMapper.toDto(message);

        // Broadcast to conversation topic
        try {
            messagingTemplate.convertAndSend("/topic/conversations." + conversationId, responseDto);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem via WebSocket para o tópico /topic/conversations.{}: {}", conversationId, e.getMessage());
        }

        return responseDto;
    }

    @Transactional
    public void markAsRead(UUID conversationId, Integer messageId) {
        User currentUser = requireCurrentUser();
        validateActiveParticipant(conversationId, currentUser.getId());

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Mensagem não encontrada", "Mensagem com ID " + messageId + " não existe"));

        ConversationParticipant cp = conversationParticipantRepository
                .findByIdConversationIdAndIdUserId(conversationId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("Acesso negado", "Participante não encontrado"));

        if (!messageReadRepository.existsByIdMessageIdAndIdUserId(messageId, currentUser.getId())) {
            MessageReadId readId = MessageReadId.builder()
                    .messageId(messageId)
                    .userId(currentUser.getId())
                    .build();

            MessageRead messageRead = MessageRead.builder()
                    .id(readId)
                    .message(message)
                    .conversationId(conversationId)
                    .conversationParticipant(cp)
                    .readAt(Instant.now())
                    .build();

            messageReadRepository.save(messageRead);
        }
    }

    public void validateActiveParticipant(UUID conversationId, UUID userId) {
        if (!conversationParticipantRepository.isUserActiveParticipant(conversationId, userId)) {
            throw new ForbiddenException("Acesso negado", "Você não é participante ativo desta conversa");
        }
    }

    private User requireCurrentUser() {
        User user = ServiceUtils.getCurrentUser();
        if (user == null) {
            throw new UnauthorizedException("Usuário não autenticado", "Faça login para continuar");
        }
        return user;
    }
}
