package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.conversation.AddParticipantRequestDto;
import com.frigus.coreapi.dto.conversation.ConversationResponseDto;
import com.frigus.coreapi.dto.conversation.CreateGroupConversationRequestDto;
import com.frigus.coreapi.dto.conversation.CreatePrivateConversationRequestDto;
import com.frigus.coreapi.dto.message.MessageResponseDto;
import com.frigus.coreapi.dto.message.MessageSendDto;
import com.frigus.coreapi.service.ConversationService;
import com.frigus.coreapi.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conversations")
@Tag(name = "Conversations", description = "Endpoints para conversas privadas (1x1) e em grupo (chat)")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @GetMapping
    @Operation(summary = "Listar conversas ativas do usuário autenticado")
    public List<ConversationResponseDto> listMyConversations() {
        return conversationService.listMyConversations();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de uma conversa por ID")
    public ConversationResponseDto getConversationById(@PathVariable UUID id) {
        return conversationService.getConversationById(id);
    }

    @PostMapping("/group")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar nova conversa de grupo (restrito a membros do grupo de pessoas)")
    public ConversationResponseDto createGroupConversation(@Valid @RequestBody CreateGroupConversationRequestDto dto) {
        return conversationService.createGroupConversation(dto);
    }

    @PostMapping("/private")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar ou obter conversa privada (1-a-1) com usuário que compartilha mesmo grupo de pessoas")
    public ConversationResponseDto getOrCreatePrivateConversation(@Valid @RequestBody CreatePrivateConversationRequestDto dto) {
        return conversationService.getOrCreatePrivateConversation(dto);
    }

    @PostMapping("/{id}/participants")
    @Operation(summary = "Adicionar participante a uma conversa em grupo")
    public ConversationResponseDto addParticipant(
            @PathVariable UUID id,
            @Valid @RequestBody AddParticipantRequestDto dto) {
        return conversationService.addParticipantToGroupConversation(id, dto);
    }

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Sair de uma conversa")
    public void leaveConversation(@PathVariable UUID id) {
        conversationService.leaveConversation(id);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Obter histórico paginado de mensagens de uma conversa")
    public Page<MessageResponseDto> getMessages(
            @PathVariable UUID id,
            Pageable pageable) {
        return messageService.getMessagesHistory(id, pageable);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar mensagem via REST (também enviada para assinantes do WebSocket)")
    public MessageResponseDto sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody MessageSendDto dto) {
        dto.setConversationId(id);
        return messageService.sendMessage(null, dto);
    }

    @PostMapping("/{id}/messages/{messageId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar mensagem como lida")
    public void markMessageAsRead(
            @PathVariable UUID id,
            @PathVariable Integer messageId) {
        messageService.markAsRead(id, messageId);
    }
}
