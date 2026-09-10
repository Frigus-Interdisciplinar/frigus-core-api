package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.conversation.AddParticipantRequestDto;
import com.frigus.coreapi.dto.conversation.ConversationResponseDto;
import com.frigus.coreapi.dto.conversation.CreateGroupConversationRequestDto;
import com.frigus.coreapi.dto.conversation.CreatePrivateConversationRequestDto;
import com.frigus.coreapi.enums.ConversationType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.mapper.ConversationMapper;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.model.ConversationParticipant;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.ConversationParticipantRepository;
import com.frigus.coreapi.repository.ConversationRepository;
import com.frigus.coreapi.repository.GroupRepository;
import com.frigus.coreapi.repository.MessageRepository;
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
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @InjectMocks
    private ConversationService conversationService;

    private User currentUser;
    private User groupMemberUser;
    private User outsiderUser;
    private Group peopleGroup;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel")
                .email("gabriel@test.com")
                .build();

        groupMemberUser = User.builder()
                .id(UUID.randomUUID())
                .name("Lucas")
                .email("lucas@test.com")
                .build();

        outsiderUser = User.builder()
                .id(UUID.randomUUID())
                .name("Estranho")
                .email("estranho@test.com")
                .build();

        peopleGroup = Group.builder()
                .id(UUID.randomUUID())
                .name("Família Gabriel")
                .build();

        var auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve criar conversa de grupo com participantes pertencentes ao grupo de pessoas")
    void shouldCreateGroupConversationSuccessfully() {
        CreateGroupConversationRequestDto dto = CreateGroupConversationRequestDto.builder()
                .groupId(peopleGroup.getId())
                .name("Chat Compras")
                .participantUserIds(List.of(groupMemberUser.getId()))
                .build();

        when(userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), peopleGroup.getId())).thenReturn(true);
        when(groupRepository.findByIdAndDeletedAtIsNull(peopleGroup.getId())).thenReturn(Optional.of(peopleGroup));
        when(userGroupRepository.existsByUserIdAndGroupId(groupMemberUser.getId(), peopleGroup.getId())).thenReturn(true);
        when(userRepository.findById(groupMemberUser.getId())).thenReturn(Optional.of(groupMemberUser));

        Conversation savedConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .conversationType(ConversationType.GROUP)
                .group(peopleGroup)
                .name("Chat Compras")
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);
        when(conversationMapper.toDto(eq(savedConversation), any(), isNull())).thenReturn(
                ConversationResponseDto.builder().id(savedConversation.getId()).name("Chat Compras").build()
        );

        ConversationResponseDto result = conversationService.createGroupConversation(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Chat Compras");
        verify(conversationParticipantRepository).saveAll(any());
    }

    @Test
    @DisplayName("Deve bloquear criação de conversa de grupo se algum participante não pertencer ao grupo de pessoas")
    void shouldThrowBadRequestWhenParticipantNotInPeopleGroup() {
        CreateGroupConversationRequestDto dto = CreateGroupConversationRequestDto.builder()
                .groupId(peopleGroup.getId())
                .name("Chat Compras")
                .participantUserIds(List.of(outsiderUser.getId()))
                .build();

        when(userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), peopleGroup.getId())).thenReturn(true);
        when(groupRepository.findByIdAndDeletedAtIsNull(peopleGroup.getId())).thenReturn(Optional.of(peopleGroup));
        when(userGroupRepository.existsByUserIdAndGroupId(outsiderUser.getId(), peopleGroup.getId())).thenReturn(false);

        Conversation savedConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .conversationType(ConversationType.GROUP)
                .group(peopleGroup)
                .name("Chat Compras")
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);

        assertThatThrownBy(() -> conversationService.createGroupConversation(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Participante inválido");
    }

    @Test
    @DisplayName("Deve criar Direct Message privada com sucesso quando usuários compartilham grupo em comum")
    void shouldCreatePrivateConversationWhenUsersShareCommonGroup() {
        CreatePrivateConversationRequestDto dto = CreatePrivateConversationRequestDto.builder()
                .targetUserId(groupMemberUser.getId())
                .build();

        when(userRepository.findById(groupMemberUser.getId())).thenReturn(Optional.of(groupMemberUser));
        when(userGroupRepository.existsCommonGroupByUsers(currentUser.getId(), groupMemberUser.getId())).thenReturn(true);

        when(conversationRepository.findPrivateConversationByUsers(currentUser.getId(), groupMemberUser.getId()))
                .thenReturn(Optional.empty());

        String pairKey = conversationService.generatePairKey(currentUser.getId(), groupMemberUser.getId());

        Conversation savedConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .conversationType(ConversationType.PRIVATE)
                .pairKey(pairKey)
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);
        when(conversationMapper.toDto(eq(savedConversation), any(), isNull())).thenReturn(
                ConversationResponseDto.builder().id(savedConversation.getId()).pairKey(pairKey).build()
        );

        ConversationResponseDto result = conversationService.getOrCreatePrivateConversation(dto);

        assertThat(result).isNotNull();
        assertThat(result.getPairKey()).isEqualTo(pairKey);
        verify(conversationParticipantRepository).saveAll(any());
    }

    @Test
    @DisplayName("Deve bloquear Direct Message privada com ForbiddenException quando usuários NÃO compartilham grupo em comum")
    void shouldThrowForbiddenWhenUsersDoNotShareCommonGroup() {
        CreatePrivateConversationRequestDto dto = CreatePrivateConversationRequestDto.builder()
                .targetUserId(outsiderUser.getId())
                .build();

        when(userRepository.findById(outsiderUser.getId())).thenReturn(Optional.of(outsiderUser));
        when(userGroupRepository.existsCommonGroupByUsers(currentUser.getId(), outsiderUser.getId())).thenReturn(false);

        assertThatThrownBy(() -> conversationService.getOrCreatePrivateConversation(dto))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Privacidade restrita");
    }

    @Test
    @DisplayName("Deve adicionar participante ao grupo de mensagens se ele fizer parte do grupo de pessoas")
    void shouldAddParticipantToGroupConversationSuccessfully() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .conversationType(ConversationType.GROUP)
                .group(peopleGroup)
                .build();

        when(conversationParticipantRepository.isUserActiveParticipant(conversationId, currentUser.getId())).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userGroupRepository.existsByUserIdAndGroupId(groupMemberUser.getId(), peopleGroup.getId())).thenReturn(true);
        when(userRepository.findById(groupMemberUser.getId())).thenReturn(Optional.of(groupMemberUser));
        when(conversationParticipantRepository.findByIdConversationIdAndIdUserId(conversationId, groupMemberUser.getId())).thenReturn(Optional.empty());

        when(conversationParticipantRepository.findActiveParticipantsByConversationId(conversationId)).thenReturn(List.of());
        when(messageRepository.findLatestMessageByConversationId(conversationId)).thenReturn(Optional.empty());
        when(conversationMapper.toDto(eq(conversation), any(), any())).thenReturn(
                ConversationResponseDto.builder().id(conversationId).build()
        );

        ConversationResponseDto result = conversationService.addParticipantToGroupConversation(
                conversationId,
                AddParticipantRequestDto.builder().userId(groupMemberUser.getId()).build()
        );

        assertThat(result).isNotNull();
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }
}
