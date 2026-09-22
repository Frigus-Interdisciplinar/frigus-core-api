package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.group.AddGroupMemberRequestDto;
import com.frigus.coreapi.dto.group.GroupCreateRequestDto;
import com.frigus.coreapi.dto.group.GroupMemberResponseDto;
import com.frigus.coreapi.dto.group.GroupResponseDto;
import com.frigus.coreapi.dto.group.GroupUpdateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.enums.ConversationType;
import com.frigus.coreapi.enums.PlanCode;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private PlanLimitsResolverService planLimitsResolverService;

    @InjectMocks
    private GroupService groupService;

    private User currentUser;
    private User otherUser;
    private Group sampleGroup;
    private Conversation sampleConversation;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel")
                .email("gabriel@test.com")
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Maria")
                .email("maria@test.com")
                .build();

        sampleGroup = Group.builder()
                .id(UUID.randomUUID())
                .owner(currentUser)
                .name("Família Silva")
                .bannerPicture("https://example.com/banner.png")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .conversationType(ConversationType.GROUP)
                .group(sampleGroup)
                .name("Família Silva")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        var auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve criar grupo de pessoas com criador como dono e conversa padrão automática")
    void shouldCreateGroupSuccessfully() {
        GroupCreateRequestDto dto = GroupCreateRequestDto.builder()
                .name("Família Silva")
                .bannerPicture("https://example.com/banner.png")
                .build();

        PlanLimitsDto limits = PlanLimitsDto.builder()
                .planCode(PlanCode.PLUS)
                .maxGroupsCreated(1)
                .maxGroupMembers(5)
                .build();

        when(planLimitsResolverService.resolveLimitsForCurrentUser()).thenReturn(limits);
        when(groupRepository.existsByOwnerIdAndDeletedAtIsNull(currentUser.getId())).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenReturn(sampleGroup);
        when(userGroupRepository.save(any(UserGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(sampleConversation);
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupResponseDto expectedDto = GroupResponseDto.builder()
                .id(sampleGroup.getId())
                .ownerId(currentUser.getId())
                .isOwner(true)
                .name(sampleGroup.getName())
                .membersCount(1)
                .defaultConversationId(sampleConversation.getId())
                .build();

        when(groupMapper.toDtoWithMembers(eq(sampleGroup), any(), eq(sampleConversation.getId()))).thenReturn(expectedDto);

        GroupResponseDto result = groupService.createGroup(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Família Silva");
        assertThat(result.getIsOwner()).isTrue();
        verify(groupRepository).save(any(Group.class));
        verify(userGroupRepository).save(any(UserGroup.class));
        verify(conversationRepository).save(any(Conversation.class));
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }

    @Test
    @DisplayName("Deve bloquear criação de grupo para usuário no plano FREE")
    void shouldThrowBadRequestWhenFreeUserAttemptsToCreateGroup() {
        GroupCreateRequestDto dto = GroupCreateRequestDto.builder()
                .name("Meu Grupo Free")
                .build();

        PlanLimitsDto freeLimits = PlanLimitsDto.builder()
                .planCode(PlanCode.FREE)
                .maxGroupsCreated(0)
                .build();

        when(planLimitsResolverService.resolveLimitsForCurrentUser()).thenReturn(freeLimits);

        assertThatThrownBy(() -> groupService.createGroup(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Criação de grupo não permitida");

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear criação de 2º grupo para assinante que já possui grupo ativo")
    void shouldThrowConflictWhenSubscriberAlreadyHasActiveGroup() {
        GroupCreateRequestDto dto = GroupCreateRequestDto.builder()
                .name("Segundo Grupo")
                .build();

        PlanLimitsDto limits = PlanLimitsDto.builder()
                .planCode(PlanCode.PLUS)
                .maxGroupsCreated(1)
                .build();

        when(planLimitsResolverService.resolveLimitsForCurrentUser()).thenReturn(limits);
        when(groupRepository.existsByOwnerIdAndDeletedAtIsNull(currentUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> groupService.createGroup(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Limite de grupos atingido");

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar os grupos do usuário logado")
    void shouldListMyGroups() {
        when(groupRepository.findGroupsByUserId(currentUser.getId())).thenReturn(List.of(sampleGroup));
        when(userGroupRepository.findByGroupId(sampleGroup.getId())).thenReturn(List.of(
                UserGroup.builder().user(currentUser).group(sampleGroup).build()
        ));
        when(conversationRepository.findByGroupId(sampleGroup.getId())).thenReturn(List.of(sampleConversation));
        when(groupMapper.toDtoWithMembers(eq(sampleGroup), any(), eq(sampleConversation.getId()))).thenReturn(
                GroupResponseDto.builder().id(sampleGroup.getId()).name(sampleGroup.getName()).build()
        );

        List<GroupResponseDto> results = groupService.listMyGroups();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Família Silva");
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException ao buscar grupo se usuário não é membro")
    void shouldThrowForbiddenWhenNotMember() {
        when(userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), sampleGroup.getId())).thenReturn(false);

        assertThatThrownBy(() -> groupService.getGroupById(sampleGroup.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Acesso negado");
    }

    @Test
    @DisplayName("Deve adicionar membro respeitando limites do plano do dono do grupo e sincronizar conversa")
    void shouldAddMemberSuccessfully() {
        when(userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), sampleGroup.getId())).thenReturn(true);
        when(groupRepository.findByIdAndDeletedAtIsNull(sampleGroup.getId())).thenReturn(Optional.of(sampleGroup));
        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(userGroupRepository.existsByUserIdAndGroupId(otherUser.getId(), sampleGroup.getId())).thenReturn(false);

        PlanLimitsDto limits = PlanLimitsDto.builder()
                .planCode(PlanCode.PLUS)
                .maxGroupMembers(5)
                .build();
        when(planLimitsResolverService.getLimitsForUser(sampleGroup.getOwner().getId())).thenReturn(limits);
        when(userGroupRepository.countByGroupId(sampleGroup.getId())).thenReturn(1);

        when(userGroupRepository.save(any(UserGroup.class))).thenAnswer(i -> i.getArgument(0));
        when(conversationRepository.findByGroupId(sampleGroup.getId())).thenReturn(List.of(sampleConversation));
        when(conversationParticipantRepository.findByIdConversationIdAndIdUserId(sampleConversation.getId(), otherUser.getId()))
                .thenReturn(Optional.empty());
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenAnswer(i -> i.getArgument(0));

        when(userGroupRepository.findByGroupId(sampleGroup.getId())).thenReturn(List.of(
                UserGroup.builder().user(currentUser).group(sampleGroup).build(),
                UserGroup.builder().user(otherUser).group(sampleGroup).build()
        ));

        GroupResponseDto responseDto = GroupResponseDto.builder()
                .id(sampleGroup.getId())
                .name(sampleGroup.getName())
                .membersCount(2)
                .defaultConversationId(sampleConversation.getId())
                .build();
        when(groupMapper.toDtoWithMembers(eq(sampleGroup), any(), eq(sampleConversation.getId()))).thenReturn(responseDto);

        GroupResponseDto result = groupService.addMember(sampleGroup.getId(), AddGroupMemberRequestDto.builder().userId(otherUser.getId()).build());

        assertThat(result).isNotNull();
        assertThat(result.getMembersCount()).isEqualTo(2);
        verify(userGroupRepository).save(any(UserGroup.class));
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }

    @Test
    @DisplayName("Deve bloquear adição de membro quando limite de membros do plano do dono for atingido")
    void shouldThrowBadRequestWhenPlanLimitReached() {
        when(userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), sampleGroup.getId())).thenReturn(true);
        when(groupRepository.findByIdAndDeletedAtIsNull(sampleGroup.getId())).thenReturn(Optional.of(sampleGroup));
        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(userGroupRepository.existsByUserIdAndGroupId(otherUser.getId(), sampleGroup.getId())).thenReturn(false);

        PlanLimitsDto limits = PlanLimitsDto.builder()
                .planCode(PlanCode.FREE)
                .maxGroupMembers(3)
                .build();
        when(planLimitsResolverService.getLimitsForUser(sampleGroup.getOwner().getId())).thenReturn(limits);
        when(userGroupRepository.countByGroupId(sampleGroup.getId())).thenReturn(3);

        assertThatThrownBy(() -> groupService.addMember(sampleGroup.getId(), AddGroupMemberRequestDto.builder().userId(otherUser.getId()).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Limite de membros atingido");
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando usuário já é membro do grupo")
    void shouldThrowConflictWhenUserAlreadyMember() {
        when(userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), sampleGroup.getId())).thenReturn(true);
        when(groupRepository.findByIdAndDeletedAtIsNull(sampleGroup.getId())).thenReturn(Optional.of(sampleGroup));
        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(userGroupRepository.existsByUserIdAndGroupId(otherUser.getId(), sampleGroup.getId())).thenReturn(true);

        assertThatThrownBy(() -> groupService.addMember(sampleGroup.getId(), AddGroupMemberRequestDto.builder().userId(otherUser.getId()).build()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Usuário já é membro");
    }

    @Test
    @DisplayName("Deve permitir exclusão do grupo pelo proprietário")
    void shouldDeleteGroupWhenOwner() {
        when(userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), sampleGroup.getId())).thenReturn(true);
        when(groupRepository.findByIdAndDeletedAtIsNull(sampleGroup.getId())).thenReturn(Optional.of(sampleGroup));

        groupService.deleteGroup(sampleGroup.getId());

        assertThat(sampleGroup.getDeletedAt()).isNotNull();
        verify(groupRepository).save(sampleGroup);
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException ao tentar excluir grupo sem ser proprietário")
    void shouldThrowForbiddenWhenNonOwnerTriesToDelete() {
        Group groupOwnedByOther = Group.builder()
                .id(UUID.randomUUID())
                .owner(otherUser)
                .name("Outro Grupo")
                .build();

        when(userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), groupOwnedByOther.getId())).thenReturn(true);
        when(groupRepository.findByIdAndDeletedAtIsNull(groupOwnedByOther.getId())).thenReturn(Optional.of(groupOwnedByOther));

        assertThatThrownBy(() -> groupService.deleteGroup(groupOwnedByOther.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Acesso negado");

        verify(groupRepository, never()).save(groupOwnedByOther);
    }
}
