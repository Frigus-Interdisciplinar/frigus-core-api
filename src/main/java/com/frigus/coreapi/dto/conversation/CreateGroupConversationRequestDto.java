package com.frigus.coreapi.dto.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupConversationRequestDto {
    @NotNull(message = "O ID do grupo é obrigatório")
    private UUID groupId;

    @NotBlank(message = "O nome da conversa é obrigatório")
    private String name;

    private List<UUID> participantUserIds;
}
