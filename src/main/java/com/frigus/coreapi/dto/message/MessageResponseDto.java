package com.frigus.coreapi.dto.message;

import com.frigus.coreapi.enums.MessageType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {
    private Integer id;
    private UUID conversationId;
    private UUID senderId;
    private String senderName;
    private MessageType messageType;
    private String content;
    private Integer relatedShoppingListProductId;
    private Instant createdAt;
    private Instant updatedAt;
}
