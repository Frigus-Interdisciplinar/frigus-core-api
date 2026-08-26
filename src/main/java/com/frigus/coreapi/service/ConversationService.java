package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.conversation.AddParticipantRequestDto;
import com.frigus.coreapi.dto.conversation.ConversationResponseDto;
import com.frigus.coreapi.dto.conversation.CreateGroupConversationRequestDto;
import com.frigus.coreapi.dto.conversation.CreatePrivateConversationRequestDto;
import com.frigus.coreapi.enums.ConversationType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.exception.UnauthorizedException;
import com.frigus.coreapi.mapper.ConversationMapper;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.model.ConversationParticipant;
import com.frigus.coreapi.model.ConversationParticipantId;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Message;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.ConversationParticipantRepository;
import com.frigus.coreapi.repository.ConversationRepository;
import com.frigus.coreapi.repository.GroupRepository;
import com.frigus.coreapi.repository.MessageRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;

    public List<ConversationResponseDto> listMyConversations() {
        User currentUser = requireCurrentUser();
        List<Conversation> conversations = conversationRepository.findActiveConversationsByUserId(currentUser.getId());

        return conversations.stream()
                .map(this::buildConversationResponseDto)
                .toList();
    }

    public ConversationResponseDto getConversationById(UUID conversationId) {
        User currentUser = requireCurrentUser();
        validateActiveParticipant(conversationId, currentUser.getId());

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada", "Conversa não existe"));

        return buildConversationResponseDto(conversation);
    }

    @Transactional
    public ConversationResponseDto createGroupConversation(CreateGroupConversationRequestDto dto) {
        User currentUser = requireCurrentUser();
        UUID groupId = dto.getGroupId();

        // Validate creator is member of the people group
        if (!userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), groupId)) {
            throw new ForbiddenException("Acesso negado", "Você não faz parte deste grupo de pessoas");
        }

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "Grupo com ID " + groupId + " não existe"));

        Conversation conversation = Conversation.builder()
                .conversationType(ConversationType.GROUP)
                .group(group)
                .name(dto.getName().trim())
                .pairKey(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        conversation = conversationRepository.save(conversation);

        // Add creator as participant
        List<ConversationParticipant> participants = new ArrayList<>();
        participants.add(createParticipantEntity(conversation, currentUser));

        // Add other invited participants if provided
        if (dto.getParticipantUserIds() != null) {
            for (UUID participantId : dto.getParticipantUserIds()) {
                if (participantId.equals(currentUser.getId())) {
                    continue;
                }

                // Strict rule: participant MUST be member of user_groups for this groupId
                if (!userGroupRepository.existsByUserIdAndGroupId(participantId, groupId)) {
                    throw new BadRequestException("Participante inválido",
                            "O usuário com ID " + participantId + " não faz parte do grupo de pessoas '" + group.getName() + "'");
                }

                User participantUser = userRepository.findById(participantId)
                        .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário com ID " + participantId + " não existe"));

                participants.add(createParticipantEntity(conversation, participantUser));
            }
        }

        conversationParticipantRepository.saveAll(participants);

        return conversationMapper.toDto(conversation, participants, null);
    }

    @Transactional
    public ConversationResponseDto getOrCreatePrivateConversation(CreatePrivateConversationRequestDto dto) {
        User currentUser = requireCurrentUser();
        UUID targetUserId = dto.getTargetUserId();

        if (currentUser.getId().equals(targetUserId)) {
            throw new BadRequestException("Conversa inválida", "Você não pode iniciar uma conversa direta consigo mesmo");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário com ID " + targetUserId + " não existe"));

        // Strict rule: users MUST share at least one active people group in common
        boolean sharesCommonGroup = userGroupRepository.existsCommonGroupByUsers(currentUser.getId(), targetUserId);
        if (!sharesCommonGroup) {
            throw new ForbiddenException("Privacidade restrita",
                    "Você só pode iniciar conversas privadas com usuários que compartilhem pelo menos um grupo de pessoas em comum com você");
        }

        Optional<Conversation> existingConversation = conversationRepository
                .findPrivateConversationByUsers(currentUser.getId(), targetUserId);

        if (existingConversation.isPresent()) {
            Conversation conversation = existingConversation.get();

            // Ensure both participants are active
            reactivateParticipantIfNeeded(conversation, currentUser);
            reactivateParticipantIfNeeded(conversation, targetUser);

            return buildConversationResponseDto(conversation);
        }

        String pairKey = generatePairKey(currentUser.getId(), targetUserId);

        // Create new private conversation
        Conversation conversation = Conversation.builder()
                .conversationType(ConversationType.PRIVATE)
                .group(null)
                .name(null)
                .pairKey(pairKey)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        conversation = conversationRepository.save(conversation);

        ConversationParticipant p1 = createParticipantEntity(conversation, currentUser);
        ConversationParticipant p2 = createParticipantEntity(conversation, targetUser);
        conversationParticipantRepository.saveAll(List.of(p1, p2));

        return conversationMapper.toDto(conversation, List.of(p1, p2), null);
    }

    @Transactional
    public ConversationResponseDto addParticipantToGroupConversation(UUID conversationId, AddParticipantRequestDto dto) {
        User currentUser = requireCurrentUser();
        validateActiveParticipant(conversationId, currentUser.getId());

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada", "Conversa não existe"));

        if (conversation.getConversationType() != ConversationType.GROUP) {
            throw new BadRequestException("Operação inválida", "Não é possível adicionar participantes a uma conversa privada");
        }

        UUID groupId = conversation.getGroup().getId();
        UUID targetUserId = dto.getUserId();

        // Strict rule: participant MUST be member of user_groups for this groupId
        if (!userGroupRepository.existsByUserIdAndGroupId(targetUserId, groupId)) {
            throw new BadRequestException("Participante inválido",
                    "O usuário não pertence ao grupo de pessoas correspondente");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário com ID " + targetUserId + " não existe"));

        Optional<ConversationParticipant> existingParticipant = conversationParticipantRepository
                .findByIdConversationIdAndIdUserId(conversationId, targetUserId);

        if (existingParticipant.isPresent()) {
            ConversationParticipant cp = existingParticipant.get();
            if (cp.getLeftAt() == null) {
                throw new ConflictException("Participante já existe", "O usuário já é participante ativo desta conversa");
            }
            cp.setLeftAt(null);
            cp.setUpdatedAt(Instant.now());
            conversationParticipantRepository.save(cp);
        } else {
            ConversationParticipant cp = createParticipantEntity(conversation, targetUser);
            conversationParticipantRepository.save(cp);
        }

        return buildConversationResponseDto(conversation);
    }

    @Transactional
    public void leaveConversation(UUID conversationId) {
        User currentUser = requireCurrentUser();
        ConversationParticipant cp = conversationParticipantRepository
                .findByIdConversationIdAndIdUserId(conversationId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Participante não encontrado", "Você não participa desta conversa"));

        cp.setLeftAt(Instant.now());
        cp.setUpdatedAt(Instant.now());
        conversationParticipantRepository.save(cp);
    }

    public void validateActiveParticipant(UUID conversationId, UUID userId) {
        if (!conversationParticipantRepository.isUserActiveParticipant(conversationId, userId)) {
            throw new ForbiddenException("Acesso negado", "Você não é participante ativo desta conversa");
        }
    }

    public String generatePairKey(UUID userA, UUID userB) {
        if (userA.compareTo(userB) < 0) {
            return userA + ":" + userB;
        } else {
            return userB + ":" + userA;
        }
    }

    private ConversationParticipant createParticipantEntity(Conversation conversation, User user) {
        ConversationParticipantId id = ConversationParticipantId.builder()
                .conversationId(conversation.getId())
                .userId(user.getId())
                .build();

        return ConversationParticipant.builder()
                .id(id)
                .conversation(conversation)
                .user(user)
                .joinedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void reactivateParticipantIfNeeded(Conversation conversation, User user) {
        Optional<ConversationParticipant> participantOpt = conversationParticipantRepository
                .findByIdConversationIdAndIdUserId(conversation.getId(), user.getId());

        if (participantOpt.isPresent()) {
            ConversationParticipant cp = participantOpt.get();
            if (cp.getLeftAt() != null) {
                cp.setLeftAt(null);
                cp.setUpdatedAt(Instant.now());
                conversationParticipantRepository.save(cp);
            }
        } else {
            ConversationParticipant cp = createParticipantEntity(conversation, user);
            conversationParticipantRepository.save(cp);
        }
    }

    private ConversationResponseDto buildConversationResponseDto(Conversation conversation) {
        List<ConversationParticipant> participants = conversationParticipantRepository
                .findActiveParticipantsByConversationId(conversation.getId());

        Message latestMessage = messageRepository
                .findLatestMessageByConversationId(conversation.getId())
                .orElse(null);

        return conversationMapper.toDto(conversation, participants, latestMessage);
    }

    private User requireCurrentUser() {
        User user = ServiceUtils.getCurrentUser();
        if (user == null) {
            throw new UnauthorizedException("Usuário não autenticado", "Faça login para continuar");
        }
        return user;
    }
}
