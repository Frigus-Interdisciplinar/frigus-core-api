package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.group.AddGroupMemberRequestDto;
import com.frigus.coreapi.dto.group.GroupCreateRequestDto;
import com.frigus.coreapi.dto.group.GroupResponseDto;
import com.frigus.coreapi.dto.group.GroupUpdateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.exception.UnauthorizedException;
import com.frigus.coreapi.mapper.GroupMapper;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.model.ConversationParticipant;
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
                    return groupMapper.toDtoWithMembers(group, members);
                })
                .toList();
    }

    public Page<GroupResponseDto> listMyGroups(Pageable pageable) {
        User currentUser = requireCurrentUser();
        return groupRepository.findGroupsByUserId(currentUser.getId(), pageable)
                .map(group -> {
                    List<UserGroup> members = userGroupRepository.findByGroupId(group.getId());
                    return groupMapper.toDtoWithMembers(group, members);
                });
    }

    public GroupResponseDto getGroupById(UUID groupId) {
        User currentUser = requireCurrentUser();
        validateUserInGroup(currentUser.getId(), groupId);

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "Grupo com ID " + groupId + " não existe"));

        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        return groupMapper.toDtoWithMembers(group, members);
    }

    @Transactional
    public GroupResponseDto createGroup(GroupCreateRequestDto dto) {
        User currentUser = requireCurrentUser();

        Group group = Group.builder()
                .name(dto.getName().trim())
                .bannerPicture(dto.getBannerPicture())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        group = groupRepository.save(group);

        UserGroup userGroup = UserGroup.builder()
                .user(currentUser)
                .group(group)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userGroupRepository.save(userGroup);

        return groupMapper.toDtoWithMembers(group, List.of(userGroup));
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
        return groupMapper.toDtoWithMembers(group, members);
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        User currentUser = requireCurrentUser();
        validateUserInGroup(currentUser.getId(), groupId);

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "Grupo com ID " + groupId + " não existe"));

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

        PlanLimitsDto limits = planLimitsResolverService.getLimitsForUser(currentUser.getId());
        int currentMemberCount = userGroupRepository.countByGroupId(groupId);
        if (currentMemberCount >= limits.getMaxGroupMembers()) {
            throw new BadRequestException("Limite de membros atingido",
                    "O plano atual permite no máximo " + limits.getMaxGroupMembers() + " membros no grupo");
        }

        UserGroup userGroup = UserGroup.builder()
                .user(targetUser)
                .group(group)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userGroupRepository.save(userGroup);

        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        return groupMapper.toDtoWithMembers(group, members);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID targetUserId) {
        User currentUser = requireCurrentUser();
        validateUserInGroup(currentUser.getId(), groupId);

        if (!userGroupRepository.existsByUserIdAndGroupId(targetUserId, groupId)) {
            throw new NotFoundException("Membro não encontrado", "O usuário não pertence a este grupo");
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
