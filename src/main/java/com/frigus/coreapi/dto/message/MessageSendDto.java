package com.frigus.coreapi.dto.message;

import com.frigus.coreapi.enums.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendDto {
    @NotNull(message = "O ID da conversa é obrigatório")
    private UUID conversationId;

    private MessageType messageType;

    private String content;

    private Integer relatedShoppingListProductId;
}
