package com.frigus.coreapi.dto.discard;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscardCreateRequestDto {

    @NotNull(message = "O produto do estoque é obrigatório")
    private Integer stockProductId;

    @Size(max = 2000, message = "O motivo deve ter no máximo 2000 caracteres")
    private String reason;
}
