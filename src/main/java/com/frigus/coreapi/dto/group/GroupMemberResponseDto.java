package com.frigus.coreapi.dto.group;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponseDto {
    private UUID userId;
    private String name;
    private String email;
    private Instant joinedAt;
}
