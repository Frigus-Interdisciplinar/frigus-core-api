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
    private UUID conversationId;
    private UUID groupId;

    private MessageType messageType;

    private String content;

    private Integer relatedShoppingListProductId;
}
