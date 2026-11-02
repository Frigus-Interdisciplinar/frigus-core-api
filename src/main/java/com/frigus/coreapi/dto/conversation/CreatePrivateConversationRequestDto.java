package com.frigus.coreapi.dto.conversation;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrivateConversationRequestDto {
    @NotNull(message = "O ID do usuário destinatário é obrigatório")
    private UUID targetUserId;
}
