package com.frigus.coreapi.dto.support;

import com.frigus.coreapi.enums.SupportRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record SupportRequestResponseDto(
        UUID id,
        String protocol,
        SupportRequestStatus status,
        String subject,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
