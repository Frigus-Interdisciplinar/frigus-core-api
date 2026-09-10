package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.message.MessageResponseDto;
import com.frigus.coreapi.dto.message.MessageSendDto;
import com.frigus.coreapi.model.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper implements BaseMapper<Message, MessageResponseDto, MessageSendDto> {

    @Override
    public MessageResponseDto toDto(Message message) {
        if (message == null) return null;

        var participant = message.getConversationParticipants();
        var sender = participant != null ? participant.getUser() : null;
        var conversation = participant != null ? participant.getConversation() : null;

        return MessageResponseDto.builder()
                .id(message.getId())
                .conversationId(conversation != null ? conversation.getId() : (participant != null && participant.getId() != null ? participant.getId().getConversationId() : null))
                .senderId(sender != null ? sender.getId() : (participant != null && participant.getId() != null ? participant.getId().getUserId() : null))
                .senderName(sender != null ? sender.getName() : null)
                .messageType(message.getMessageType())
                .content(message.getContent())
                .relatedShoppingListProductId(message.getRelatedShoppingListProduct() != null ? message.getRelatedShoppingListProduct().getId() : null)
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    @Override
    public Message toEntity(MessageSendDto dto) {
        if (dto == null) return null;
        return Message.builder()
                .messageType(dto.getMessageType())
                .content(dto.getContent())
                .build();
    }
}
