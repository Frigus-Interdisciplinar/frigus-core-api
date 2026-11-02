package com.frigus.coreapi.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiRecipeChatRequestDto {

    @NotBlank(message = "A mensagem é obrigatória")
    private String message;

    @NotNull(message = "O ID do estoque é obrigatório")
    private Integer stockId;

    private String sessionId;

    @Size(max = 9)
    private List<String> foodPreferences;
}
