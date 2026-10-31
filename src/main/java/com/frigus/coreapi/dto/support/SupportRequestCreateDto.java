package com.frigus.coreapi.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportRequestCreateDto(
        @NotBlank @Size(min = 3, max = 120) String subject,
        @NotBlank @Size(min = 10, max = 5000) String description
) {}
