package com.frigus.coreapi.dto.conversation;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponseDto {
    private UUID userId;
    private String name;
    private String email;
    private Instant joinedAt;
    private Instant leftAt;
}
