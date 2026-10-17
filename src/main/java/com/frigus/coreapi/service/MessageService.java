package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.message.MessageResponseDto;
import com.frigus.coreapi.dto.message.MessageSendDto;
import com.frigus.coreapi.dto.message.MessageAttachmentInputDto;
import com.frigus.coreapi.dto.message.MessageAttachmentResponseDto;
import com.frigus.coreapi.dto.message.MessageReactionDto;
import com.frigus.coreapi.enums.MessageType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.exception.UnauthorizedException;
import com.frigus.coreapi.mapper.MessageMapper;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.model.ConversationParticipant;
import com.frigus.coreapi.model.Message;
import com.frigus.coreapi.model.MessageRead;
import com.frigus.coreapi.model.MessageReadId;
import com.frigus.coreapi.model.MessageAttachment;
import com.frigus.coreapi.model.MessageReaction;
import com.frigus.coreapi.model.ShoppingListProduct;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.ConversationParticipantRepository;
import com.frigus.coreapi.repository.ConversationRepository;
import com.frigus.coreapi.repository.MessageReadRepository;
import com.frigus.coreapi.repository.MessageRepository;
import com.frigus.coreapi.repository.MessageAttachmentRepository;
import com.frigus.coreapi.repository.MessageReactionRepository;
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
import java.util.List;
import java.util.Set;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageReadRepository messageReadRepository;
    private final ShoppingListProductRepository shoppingListProductRepository;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public Page<MessageResponseDto> getMessagesHistory(UUID conversationId, Pageable pageable) {
        return getMessagesHistory(conversationId, pageable, null);
    }

    public Page<MessageResponseDto> getMessagesHistory(UUID conversationId, Pageable pageable, String search) {
        User currentUser = requireCurrentUser();
        validateActiveParticipant(conversationId, currentUser.getId());
        var messages = search == null || search.isBlank()
                ? messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                : messageRepository.searchByConversationId(conversationId, search.trim(), pageable);
        return messages.map(message -> withAttachments(messageMapper.toDto(message), message));
    }

    @Transactional
    public MessageResponseDto sendMessage(User sender, MessageSendDto dto) {
        User actualSender = sender != null ? sender : requireCurrentUser();
        UUID resolvedConversationId = dto.getConversationId();

        // Se conversationId não foi passado, mas groupId foi informado, busca a conversa do grupo
        if (resolvedConversationId == null && dto.getGroupId() != null) {
            UUID groupId = dto.getGroupId();
            Conversation groupConv = conversationRepository.findByGroupId(groupId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Conversa do grupo não encontrada", 
                            "Nenhuma conversa encontrada para o grupo com ID " + groupId));
            resolvedConversationId = groupConv.getId();
        }

        if (resolvedConversationId == null) {
            throw new BadRequestException("Parâmetro inválido", "Informe o conversationId ou o groupId para enviar a mensagem");
        }

        final UUID conversationId = resolvedConversationId;

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

        List<MessageAttachmentInputDto> requestedAttachments = dto.getAttachments() == null ? List.of() : dto.getAttachments();
        if (requestedAttachments.size() > 5) {
            throw new BadRequestException("Anexos demais", "Envie no máximo 5 anexos por mensagem.");
        }
        for (MessageAttachmentInputDto attachment : requestedAttachments) {
            validateAttachment(attachment, conversationId, actualSender.getId());
            messageAttachmentRepository.save(MessageAttachment.builder()
                    .message(message)
                    .fileUrl(attachment.getFileUrl())
                    .fileName(attachment.getFileName())
                    .mimeType(attachment.getMimeType())
                    .fileSize(attachment.getFileSize())
                    .createdAt(Instant.now())
                    .build());
        }

        // Update conversation timestamp
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        MessageResponseDto responseDto = withAttachments(messageMapper.toDto(message), message);

        // Broadcast to conversation topic
        try {
            messagingTemplate.convertAndSend("/topic/conversations." + conversationId, responseDto);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem via WebSocket para o tópico /topic/conversations.{}: {}", conversationId, e.getMessage());
        }

        // Broadcast to group topic if it's a group conversation
        if (conversation.getGroup() != null && conversation.getGroup().getId() != null) {
            UUID groupId = conversation.getGroup().getId();
            try {
                messagingTemplate.convertAndSend("/topic/groups." + groupId, responseDto);
                messagingTemplate.convertAndSend("/topic/groups." + groupId + ".messages", responseDto);
            } catch (Exception e) {
                log.error("Erro ao enviar mensagem via WebSocket para os tópicos do grupo {}: {}", groupId, e.getMessage());
            }
        }

        return responseDto;
    }

    @Transactional(readOnly = true)
    public List<MessageReactionDto> listReactions(UUID conversationId, Integer messageId) {
        User user = requireCurrentUser();
        validateActiveParticipant(conversationId, user.getId());
        requireMessage(conversationId, messageId);
        return messageReactionRepository.findByMessage_IdOrderByCreatedAtAsc(messageId).stream()
                .map(r -> MessageReactionDto.builder().userId(r.getUser().getId()).emoji(r.getEmoji()).build()).toList();
    }

    @Transactional
    public List<MessageReactionDto> addReaction(UUID conversationId, Integer messageId, String emoji) {
        User user = requireCurrentUser();
        validateActiveParticipant(conversationId, user.getId());
        Message message = requireMessage(conversationId, messageId);
        String normalized = normalizeEmoji(emoji);
        if (messageReactionRepository.findByMessage_IdAndUser_IdAndEmoji(messageId, user.getId(), normalized).isEmpty()) {
            try {
                messageReactionRepository.save(MessageReaction.builder().message(message).user(user).emoji(normalized).createdAt(Instant.now()).build());
            } catch (DataIntegrityViolationException ignored) { /* duplicate concurrent reaction */ }
        }
        return listReactions(conversationId, messageId);
    }

    @Transactional
    public List<MessageReactionDto> removeReaction(UUID conversationId, Integer messageId, String emoji) {
        User user = requireCurrentUser();
        validateActiveParticipant(conversationId, user.getId());
        requireMessage(conversationId, messageId);
        messageReactionRepository.findByMessage_IdAndUser_IdAndEmoji(messageId, user.getId(), normalizeEmoji(emoji))
                .ifPresent(messageReactionRepository::delete);
        return listReactions(conversationId, messageId);
    }

    private Message requireMessage(UUID conversationId, Integer messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Mensagem não encontrada", "Mensagem não existe"));
        if (!conversationId.equals(message.getConversationParticipants().getId().getConversationId())) {
            throw new NotFoundException("Mensagem não encontrada", "Mensagem não pertence a esta conversa");
        }
        return message;
    }

    private String normalizeEmoji(String emoji) {
        Set<String> supported = Set.of("👍", "❤️", "😂", "😮", "😢", "🙏");
        if (emoji == null || !supported.contains(emoji.trim())) {
            throw new BadRequestException("Reação inválida", "Escolha uma das reações disponíveis.");
        }
        return emoji.trim();
    }

    private void validateAttachment(MessageAttachmentInputDto file, UUID conversationId, UUID userId) {
        Set<String> supported = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
        String prefix = "chat/" + conversationId + "/" + userId + "/";
        if (file == null || file.getFileUrl() == null || !file.getFileUrl().startsWith(prefix) ||
                file.getFileUrl().contains("..") || file.getFileName() == null || file.getFileName().isBlank() ||
                file.getMimeType() == null || !supported.contains(file.getMimeType().toLowerCase(Locale.ROOT)) ||
                file.getFileSize() == null || file.getFileSize() < 1 || file.getFileSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("Anexo inválido", "O anexo deve ser enviado para a conversa autenticada, ter um tipo permitido e possuir até 10 MB.");
        }
    }

    private MessageResponseDto withAttachments(MessageResponseDto dto, Message message) {
        dto.setAttachments(messageAttachmentRepository.findByMessage_IdOrderByIdAsc(message.getId()).stream()
                .map(file -> MessageAttachmentResponseDto.builder().id(file.getId()).fileUrl(file.getFileUrl())
                        .fileName(file.getFileName()).mimeType(file.getMimeType()).fileSize(file.getFileSize())
                        .createdAt(file.getCreatedAt()).build()).toList());
        return dto;
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
