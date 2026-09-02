package com.frigus.coreapi.dto.group;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddGroupMemberRequestDto {
    @NotNull(message = "O ID do usuário é obrigatório")
    private UUID userId;
}
