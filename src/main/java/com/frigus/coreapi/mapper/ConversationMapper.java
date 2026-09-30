package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.conversation.ConversationResponseDto;
import com.frigus.coreapi.dto.conversation.ParticipantResponseDto;
import com.frigus.coreapi.dto.message.MessageResponseDto;
import com.frigus.coreapi.model.Conversation;
import com.frigus.coreapi.model.ConversationParticipant;
import com.frigus.coreapi.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationMapper {

    private final MessageMapper messageMapper;

    public ConversationResponseDto toDto(Conversation conversation,
                                         List<ConversationParticipant> participants,
                                         Message latestMessage) {
        if (conversation == null) return null;

        List<ParticipantResponseDto> participantDtos = participants != null ? participants.stream()
                .map(this::toParticipantDto)
                .toList() : List.of();

        MessageResponseDto latestMessageDto = latestMessage != null ? messageMapper.toDto(latestMessage) : null;

        return ConversationResponseDto.builder()
                .id(conversation.getId())
                .conversationType(conversation.getConversationType())
                .groupId(conversation.getGroup() != null ? conversation.getGroup().getId() : null)
                .groupName(conversation.getGroup() != null ? conversation.getGroup().getName() : null)
                .name(conversation.getName())
                .pairKey(conversation.getPairKey())
                .participants(participantDtos)
                .latestMessage(latestMessageDto)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    public ParticipantResponseDto toParticipantDto(ConversationParticipant participant) {
        if (participant == null || participant.getUser() == null) return null;
        return ParticipantResponseDto.builder()
                .userId(participant.getUser().getId())
                .name(participant.getUser().getName())
                .email(participant.getUser().getEmail())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .build();
    }
}
