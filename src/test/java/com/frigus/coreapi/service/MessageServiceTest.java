package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.message.MessageResponseDto;
import com.frigus.coreapi.dto.message.MessageSendDto;
import com.frigus.coreapi.enums.MessageType;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.mapper.MessageMapper;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.model.ConversationParticipant;
import com.frigus.coreapi.model.ConversationParticipantId;
import com.frigus.coreapi.model.Message;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.ConversationParticipantRepository;
import com.frigus.coreapi.repository.ConversationRepository;
import com.frigus.coreapi.repository.MessageReadRepository;
import com.frigus.coreapi.repository.MessageRepository;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private MessageReadRepository messageReadRepository;

    @Mock
    private ShoppingListProductRepository shoppingListProductRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    private User currentUser;
    private Conversation sampleConversation;
    private ConversationParticipant sampleParticipant;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel")
                .email("gabriel@test.com")
                .build();

        sampleConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .build();

        sampleParticipant = ConversationParticipant.builder()
                .id(new ConversationParticipantId(sampleConversation.getId(), currentUser.getId()))
                .conversation(sampleConversation)
                .user(currentUser)
                .build();

        var auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve enviar mensagem com sucesso e publicar no WebSocket")
    void shouldSendMessageSuccessfully() {
        MessageSendDto dto = MessageSendDto.builder()
                .conversationId(sampleConversation.getId())
                .messageType(MessageType.TEXT)
                .content("Olá família!")
                .build();

        when(conversationRepository.findById(sampleConversation.getId())).thenReturn(Optional.of(sampleConversation));
        when(conversationParticipantRepository.findByIdConversationIdAndIdUserId(sampleConversation.getId(), currentUser.getId()))
                .thenReturn(Optional.of(sampleParticipant));

        Message savedMessage = Message.builder()
                .id(1)
                .conversationParticipants(sampleParticipant)
                .content("Olá família!")
                .messageType(MessageType.TEXT)
                .createdAt(Instant.now())
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        MessageResponseDto responseDto = MessageResponseDto.builder()
                .id(1)
                .conversationId(sampleConversation.getId())
                .senderId(currentUser.getId())
                .content("Olá família!")
                .build();
        when(messageMapper.toDto(savedMessage)).thenReturn(responseDto);

        MessageResponseDto result = messageService.sendMessage(currentUser, dto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Olá família!");
        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations." + sampleConversation.getId()), eq(responseDto));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException ao tentar enviar mensagem para conversa onde não é participante")
    void shouldThrowForbiddenWhenNotParticipant() {
        MessageSendDto dto = MessageSendDto.builder()
                .conversationId(sampleConversation.getId())
                .content("Olá")
                .build();

        when(conversationRepository.findById(sampleConversation.getId())).thenReturn(Optional.of(sampleConversation));
        when(conversationParticipantRepository.findByIdConversationIdAndIdUserId(sampleConversation.getId(), currentUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(currentUser, dto))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Acesso negado");
    }

    @Test
    @DisplayName("Deve retornar histórico paginado de mensagens")
    void shouldGetMessagesHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        when(conversationParticipantRepository.isUserActiveParticipant(sampleConversation.getId(), currentUser.getId())).thenReturn(true);

        Message msg = Message.builder().id(1).content("Mensagem").build();
        Page<Message> page = new PageImpl<>(List.of(msg));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(sampleConversation.getId(), pageable)).thenReturn(page);
        when(messageMapper.toDto(msg)).thenReturn(MessageResponseDto.builder().id(1).content("Mensagem").build());

        Page<MessageResponseDto> result = messageService.getMessagesHistory(sampleConversation.getId(), pageable);

        assertThat(result).hasSize(1);
    }
}
