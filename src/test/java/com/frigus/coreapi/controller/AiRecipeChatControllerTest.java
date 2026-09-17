package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.ai.AiRecipeChatRequestDto;
import com.frigus.coreapi.dto.ai.AiRecipeChatResponseDto;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.AiRecipeChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecipeChatControllerTest {

    @Mock
    private AiRecipeChatService aiRecipeChatService;

    @InjectMocks
    private AiRecipeChatController controller;

    @Test
    @DisplayName("Deve processar mensagem do chatbot de receitas com sucesso")
    void shouldChatSuccessfully() {
        User user = User.builder().id(UUID.randomUUID()).build();
        AiRecipeChatRequestDto request = AiRecipeChatRequestDto.builder()
                .message("O que fazer?")
                .stockId(1)
                .build();

        AiRecipeChatResponseDto expected = AiRecipeChatResponseDto.builder()
                .chatMessage("Sugestão de receita")
                .recipeName("Torta")
                .build();

        when(aiRecipeChatService.processUserMessage(eq(user), eq(request))).thenReturn(expected);

        ResponseEntity<AiRecipeChatResponseDto> response = controller.chat(request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRecipeName()).isEqualTo("Torta");
        verify(aiRecipeChatService).processUserMessage(eq(user), eq(request));
    }

    @Test
    @DisplayName("Deve listar sugestões por estoque com sucesso")
    void shouldGetSuggestionsByStock() {
        User user = User.builder().id(UUID.randomUUID()).build();
        AiRecipeChatResponseDto dto = AiRecipeChatResponseDto.builder().recipeName("Sopa").build();
        when(aiRecipeChatService.getSuggestionsByStock(eq(user), eq(1))).thenReturn(List.of(dto));

        ResponseEntity<List<AiRecipeChatResponseDto>> response = controller.getSuggestionsByStock(1, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(aiRecipeChatService).getSuggestionsByStock(eq(user), eq(1));
    }
}
