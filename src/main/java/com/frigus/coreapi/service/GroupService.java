package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.group.AddGroupMemberRequestDto;
import com.frigus.coreapi.dto.group.GroupCreateRequestDto;
import com.frigus.coreapi.dto.group.GroupResponseDto;
import com.frigus.coreapi.dto.group.GroupUpdateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.enums.ConversationType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.exception.UnauthorizedException;
import com.frigus.coreapi.mapper.GroupMapper;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.model.ConversationParticipant;
import com.frigus.coreapi.model.ConversationParticipantId;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.model.UserGroup;
import com.frigus.coreapi.repository.ConversationParticipantRepository;
import com.frigus.coreapi.repository.ConversationRepository;
import com.frigus.coreapi.repository.GroupRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final GroupMapper groupMapper;
    private final PlanLimitsResolverService planLimitsResolverService;

    public List<GroupResponseDto> listMyGroups() {
        User currentUser = requireCurrentUser();
        List<Group> groups = groupRepository.findGroupsByUserId(currentUser.getId());
        return groups.stream()
                .map(group -> {
                    List<UserGroup> members = userGroupRepository.findByGroupId(group.getId());
                    UUID defaultConvId = conversationRepository.findByGroupId(group.getId())
                            .stream().findFirst().map(Conversation::getId).orElse(null);
                    return groupMapper.toDtoWithMembers(group, members, defaultConvId);
                })
                .toList();
    }

    public Page<GroupResponseDto> listMyGroups(Pageable pageable) {
        User currentUser = requireCurrentUser();
        return groupRepository.findGroupsByUserId(currentUser.getId(), pageable)
                .map(group -> {
                    List<UserGroup> members = userGroupRepository.findByGroupId(group.getId());
                    UUID defaultConvId = conversationRepository.findByGroupId(group.getId())
                            .stream().findFirst().map(Conversation::getId).orElse(null);
                    return groupMapper.toDtoWithMembers(group, members, defaultConvId);
                });
    }

    public GroupResponseDto getGroupById(UUID groupId) {
        User currentUser = requireCurrentUser();
        validateUserInGroup(currentUser.getId(), groupId);

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "Grupo com ID " + groupId + " não existe"));

        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        UUID defaultConvId = conversationRepository.findByGroupId(groupId)
                .stream().findFirst().map(Conversation::getId).orElse(null);
        return groupMapper.toDtoWithMembers(group, members, defaultConvId);
    }

    @Transactional
    public GroupResponseDto createGroup(GroupCreateRequestDto dto) {
        User currentUser = requireCurrentUser();

        // 1. Validar se o plano do usuário permite criar grupos
        PlanLimitsDto limits = planLimitsResolverService.resolveLimitsForCurrentUser();
        if (limits.getMaxGroupsCreated() <= 0) {
            throw new BadRequestException("Criação de grupo não permitida",
                    "Usuários no plano gratuito não podem criar grupos próprios. Assine um plano para criar seu grupo.");
        }

        // 2. Validar se o usuário já participa de algum grupo ativo
        if (userGroupRepository.existsByUserIdAndGroupDeletedAtIsNull(currentUser.getId())) {
            throw new ConflictException("Usuário já pertence a um grupo",
                    "Você já faz parte de um grupo ativo. Saia do seu grupo atual antes de criar um novo.");
        }

        // 3. Validar se o usuário já possui 1 grupo ativo criado
        if (groupRepository.existsByOwnerIdAndDeletedAtIsNull(currentUser.getId())) {
            throw new ConflictException("Limite de grupos atingido",
                    "Você já possui um grupo ativo criado. Cada usuário só pode criar 1 grupo.");
        }

        // 3. Criar grupo vinculando o proprietário
        Group group = Group.builder()
                .owner(currentUser)
                .name(dto.getName().trim())
                .bannerPicture(dto.getBannerPicture())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        group = groupRepository.save(group);

        // 4. Inserir criador como membro do grupo
        UserGroup userGroup = UserGroup.builder()
                .user(currentUser)
                .group(group)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userGroupRepository.save(userGroup);

        // 5. Criar conversa padrão de grupo automaticamente e associar o criador
        Conversation defaultConversation = Conversation.builder()
                .conversationType(ConversationType.GROUP)
                .group(group)
                .name(group.getName())
                .pairKey(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        defaultConversation = conversationRepository.save(defaultConversation);

        ConversationParticipant cp = ConversationParticipant.builder()
                .id(new ConversationParticipantId(defaultConversation.getId(), currentUser.getId()))
                .conversation(defaultConversation)
                .user(currentUser)
                .joinedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        conversationParticipantRepository.save(cp);

        return groupMapper.toDtoWithMembers(group, List.of(userGroup), defaultConversation.getId());
    }

    @Transactional
    public GroupResponseDto updateGroup(UUID groupId, GroupUpdateRequestDto dto) {
        User currentUser = requireCurrentUser();
        validateUserInGroup(currentUser.getId(), groupId);

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "Grupo com ID " + groupId + " não existe"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            group.setName(dto.getName().trim());
        }
        if (dto.getBannerPicture() != null) {
            group.setBannerPicture(dto.getBannerPicture());
        }

        group.setUpdatedAt(Instant.now());
        group = groupRepository.save(group);

        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        UUID defaultConvId = conversationRepository.findByGroupId(groupId)
                .stream().findFirst().map(Conversation::getId).orElse(null);
        return groupMapper.toDtoWithMembers(group, members, defaultConvId);
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        User currentUser = requireCurrentUser();
        validateUserInGroup(currentUser.getId(), groupId);

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "Grupo com ID " + groupId + " não existe"));

        if (group.getOwner() != null && !group.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Acesso negado", "Apenas o proprietário pode excluir este grupo");
        }

        group.setDeletedAt(Instant.now());
        group.setUpdatedAt(Instant.now());
        groupRepository.save(group);
    }

    @Transactional
    public GroupResponseDto addMember(UUID groupId, AddGroupMemberRequestDto dto) {
        User currentUser = requireCurrentUser();
        validateUserInGroup(currentUser.getId(), groupId);

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "Grupo com ID " + groupId + " não existe"));

        User targetUser = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário com ID " + dto.getUserId() + " não existe"));

        if (userGroupRepository.existsByUserIdAndGroupId(targetUser.getId(), groupId)) {
            throw new ConflictException("Usuário já é membro", "O usuário já faz parte deste grupo de pessoas");
        }

        // Regra: 1 pessoa só pode fazer parte de um grupo por vez
        if (userGroupRepository.existsByUserIdAndGroupDeletedAtIsNull(targetUser.getId())) {
            throw new ConflictException("Usuário já pertence a um grupo",
                    "O usuário já faz parte de outro grupo ativo. Cada usuário só pode pertencer a 1 grupo por vez.");
        }

        // Validação de limite de membros baseada no plano do proprietário do grupo
        UUID ownerId = group.getOwner() != null ? group.getOwner().getId() : currentUser.getId();
        PlanLimitsDto limits = planLimitsResolverService.getLimitsForUser(ownerId);
        int currentMemberCount = userGroupRepository.countByGroupId(groupId);
        if (currentMemberCount >= limits.getMaxGroupMembers()) {
            throw new BadRequestException("Limite de membros atingido",
                    "O plano do proprietário do grupo permite no máximo " + limits.getMaxGroupMembers() + " membros no grupo");
        }

        UserGroup userGroup = UserGroup.builder()
                .user(targetUser)
                .group(group)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userGroupRepository.save(userGroup);

        // Sincronizar participante em todas as conversas ativas do grupo
        List<Conversation> groupConversations = conversationRepository.findByGroupId(groupId);
        for (Conversation conversation : groupConversations) {
            Optional<ConversationParticipant> existingParticipant = conversationParticipantRepository
                    .findByIdConversationIdAndIdUserId(conversation.getId(), targetUser.getId());
            if (existingParticipant.isPresent()) {
                ConversationParticipant participant = existingParticipant.get();
                participant.setLeftAt(null);
                participant.setUpdatedAt(Instant.now());
                conversationParticipantRepository.save(participant);
            } else {
                ConversationParticipant participant = ConversationParticipant.builder()
                        .id(new ConversationParticipantId(conversation.getId(), targetUser.getId()))
                        .conversation(conversation)
                        .user(targetUser)
                        .joinedAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                conversationParticipantRepository.save(participant);
            }
        }

        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        UUID defaultConvId = groupConversations.stream().findFirst().map(Conversation::getId).orElse(null);
        return groupMapper.toDtoWithMembers(group, members, defaultConvId);
    }

    @Transactional
    public void leaveGroup(UUID groupId) {
        User currentUser = requireCurrentUser();
        removeMember(groupId, currentUser.getId());
    }

    @Transactional
    public void removeMember(UUID groupId, UUID targetUserId) {
        User currentUser = requireCurrentUser();
        validateUserInGroup(currentUser.getId(), groupId);

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "Grupo com ID " + groupId + " não existe"));

        if (!userGroupRepository.existsByUserIdAndGroupId(targetUserId, groupId)) {
            throw new NotFoundException("Membro não encontrado", "O usuário não pertence a este grupo");
        }

        boolean isTargetOwner = group.getOwner() != null && group.getOwner().getId().equals(targetUserId);
        boolean isSelf = currentUser.getId().equals(targetUserId);
        boolean isCurrentOwner = group.getOwner() != null && group.getOwner().getId().equals(currentUser.getId());

        // Se não for o próprio usuário saindo, apenas o proprietário pode remover outros membros
        if (!isSelf && !isCurrentOwner) {
            throw new ForbiddenException("Acesso negado", "Apenas o proprietário pode remover outros membros do grupo");
        }

        // Membros comuns não podem expulsar o proprietário
        if (isTargetOwner && !isSelf) {
            throw new ForbiddenException("Acesso negado", "O proprietário do grupo não pode ser removido por outros membros");
        }

        if (isTargetOwner) {
            // O admin está saindo do grupo
            List<UserGroup> otherMembers = userGroupRepository.findByGroupIdAndUserIdNot(groupId, targetUserId);

            if (otherMembers.isEmpty()) {
                // Não há mais membros: grupo deletado
                group.setDeletedAt(Instant.now());
                group.setUpdatedAt(Instant.now());
                groupRepository.save(group);
            } else {
                // Um membro aleatório recebe o admin
                int randomIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(otherMembers.size());
                User newOwner = otherMembers.get(randomIndex).getUser();
                group.setOwner(newOwner);
                group.setUpdatedAt(Instant.now());
                groupRepository.save(group);
            }
        } else {
            // Membro comum saindo ou sendo expulso. Se não restar nenhum membro no grupo, deleta o grupo
            int remainingCount = userGroupRepository.countByGroupId(groupId) - 1;
            if (remainingCount <= 0) {
                group.setDeletedAt(Instant.now());
                group.setUpdatedAt(Instant.now());
                groupRepository.save(group);
            }
        }

        userGroupRepository.deleteByUserIdAndGroupId(targetUserId, groupId);

        // Leave any group conversations associated with this group
        List<Conversation> groupConversations = conversationRepository.findByGroupId(groupId);
        for (Conversation conversation : groupConversations) {
            conversationParticipantRepository.findByIdConversationIdAndIdUserId(conversation.getId(), targetUserId)
                    .ifPresent(cp -> {
                        cp.setLeftAt(Instant.now());
                        cp.setUpdatedAt(Instant.now());
                        conversationParticipantRepository.save(cp);
                    });
        }
    }

    public void validateUserInGroup(UUID userId, UUID groupId) {
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new ForbiddenException("Acesso negado", "Você não é membro deste grupo de pessoas");
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
