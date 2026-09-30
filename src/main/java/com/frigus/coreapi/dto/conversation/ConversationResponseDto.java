package com.frigus.coreapi.dto.conversation;

import com.frigus.coreapi.dto.message.MessageResponseDto;
import com.frigus.coreapi.enums.ConversationType;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDto {
    private UUID id;
    private ConversationType conversationType;
    private UUID groupId;
    private String groupName;
    private String name;
    private String pairKey;
    private List<ParticipantResponseDto> participants;
    private MessageResponseDto latestMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
