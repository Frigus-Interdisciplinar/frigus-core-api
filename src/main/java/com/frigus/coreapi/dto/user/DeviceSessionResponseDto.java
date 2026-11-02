package com.frigus.coreapi.dto.user;

import java.time.Instant;
import java.util.UUID;

public record DeviceSessionResponseDto(
        UUID id,
        String deviceName,
        Instant createdAt,
        Instant lastActiveAt,
        boolean current,
        String location
) {}
