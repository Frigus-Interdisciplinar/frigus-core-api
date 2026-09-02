package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.message.MessageSendDto;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void handleSendMessage(@Payload MessageSendDto dto, Principal principal) {
        User sender = null;
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            sender = user;
        }

        log.debug("Mensagem recebida via WebSocket de: {}", sender != null ? sender.getEmail() : "anônimo");
        messageService.sendMessage(sender, dto);
    }
}
