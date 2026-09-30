package com.frigus.coreapi.dto.conversation;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddParticipantRequestDto {
    @NotNull(message = "O ID do usuário é obrigatório")
    private UUID userId;
}
