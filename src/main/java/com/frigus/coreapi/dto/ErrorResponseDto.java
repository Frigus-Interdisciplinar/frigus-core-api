package com.frigus.coreapi.dto;

import java.time.Instant;

public record ErrorResponseDto(
        int status,
        String code,
        String message,
        String displayMessage,
        Instant timestamp
) {}