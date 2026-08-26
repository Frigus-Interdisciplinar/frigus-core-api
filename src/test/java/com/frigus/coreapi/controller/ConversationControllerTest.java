package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.conversation.AddParticipantRequestDto;
import com.frigus.coreapi.dto.conversation.ConversationResponseDto;
import com.frigus.coreapi.dto.conversation.CreateGroupConversationRequestDto;
import com.frigus.coreapi.dto.conversation.CreatePrivateConversationRequestDto;
import com.frigus.coreapi.dto.message.MessageResponseDto;
import com.frigus.coreapi.dto.message.MessageSendDto;
import com.frigus.coreapi.enums.ConversationType;
import com.frigus.coreapi.service.ConversationService;
import com.frigus.coreapi.service.MessageService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ConversationController conversationController;

    @Test
    @DisplayName("Deve listar conversas do usuário")
    void shouldListConversations() {
        ConversationResponseDto dto = ConversationResponseDto.builder()
                .id(UUID.randomUUID())
                .conversationType(ConversationType.GROUP)
                .name("Chat Geral")
                .build();

        when(conversationService.listMyConversations()).thenReturn(List.of(dto));

        List<ConversationResponseDto> result = conversationController.listMyConversations();

        assertThat(result).hasSize(1);
        verify(conversationService).listMyConversations();
    }

    @Test
    @DisplayName("Deve criar conversa de grupo")
    void shouldCreateGroupConversation() {
        CreateGroupConversationRequestDto request = CreateGroupConversationRequestDto.builder()
                .groupId(UUID.randomUUID())
                .name("Chat Geral")
                .build();

        ConversationResponseDto expected = ConversationResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Chat Geral")
                .build();

        when(conversationService.createGroupConversation(request)).thenReturn(expected);

        ConversationResponseDto result = conversationController.createGroupConversation(request);

        assertThat(result).isNotNull();
        verify(conversationService).createGroupConversation(request);
    }

    @Test
    @DisplayName("Deve criar/obter conversa privada (DM)")
    void shouldCreatePrivateConversation() {
        CreatePrivateConversationRequestDto request = CreatePrivateConversationRequestDto.builder()
                .targetUserId(UUID.randomUUID())
                .build();

        ConversationResponseDto expected = ConversationResponseDto.builder()
                .id(UUID.randomUUID())
                .conversationType(ConversationType.PRIVATE)
                .build();

        when(conversationService.getOrCreatePrivateConversation(request)).thenReturn(expected);

        ConversationResponseDto result = conversationController.getOrCreatePrivateConversation(request);

        assertThat(result).isNotNull();
        verify(conversationService).getOrCreatePrivateConversation(request);
    }

    @Test
    @DisplayName("Deve enviar mensagem e chamar messageService")
    void shouldSendMessage() {
        UUID conversationId = UUID.randomUUID();
        MessageSendDto request = MessageSendDto.builder()
                .content("Olá!")
                .build();

        MessageResponseDto expected = MessageResponseDto.builder()
                .id(1)
                .conversationId(conversationId)
                .content("Olá!")
                .build();

        when(messageService.sendMessage(isNull(), eq(request))).thenReturn(expected);

        MessageResponseDto result = conversationController.sendMessage(conversationId, request);

        assertThat(result).isNotNull();
        assertThat(request.getConversationId()).isEqualTo(conversationId);
        verify(messageService).sendMessage(isNull(), eq(request));
    }

    @Test
    @DisplayName("Deve buscar histórico de mensagens")
    void shouldGetMessages() {
        UUID conversationId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<MessageResponseDto> expectedPage = new PageImpl<>(List.of(
                MessageResponseDto.builder().id(1).content("Olá").build()
        ));

        when(messageService.getMessagesHistory(conversationId, pageable)).thenReturn(expectedPage);

        Page<MessageResponseDto> result = conversationController.getMessages(conversationId, pageable);

        assertThat(result).hasSize(1);
        verify(messageService).getMessagesHistory(conversationId, pageable);
    }
}
