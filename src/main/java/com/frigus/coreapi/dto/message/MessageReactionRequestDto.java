package com.frigus.coreapi.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageReactionRequestDto(
        @NotBlank @Size(max = 16) String emoji
) {}
