package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.ai.AiRecipeChatRequestDto;
import com.frigus.coreapi.dto.ai.AiRecipeChatResponseDto;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.AiRecipeChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/recipes")
public class AiRecipeChatController{

    private final AiRecipeChatService aiRecipeChatService;

    @PostMapping("/chat")
    public ResponseEntity<AiRecipeChatResponseDto> chat(
            @Valid @RequestBody AiRecipeChatRequestDto dto,
            @AuthenticationPrincipal User currentUser) {
        AiRecipeChatResponseDto response = aiRecipeChatService.processUserMessage(currentUser, dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions/stock/{stockId}")
    public ResponseEntity<List<AiRecipeChatResponseDto>> getSuggestionsByStock(
            @PathVariable Integer stockId,
            @AuthenticationPrincipal User currentUser) {
        List<AiRecipeChatResponseDto> response = aiRecipeChatService.getSuggestionsByStock(currentUser, stockId);
        return ResponseEntity.ok(response);
    }
}
