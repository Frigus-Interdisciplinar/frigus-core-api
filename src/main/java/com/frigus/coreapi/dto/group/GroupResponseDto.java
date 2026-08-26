package com.frigus.coreapi.dto.group;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponseDto {
    private UUID id;
    private String name;
    private String bannerPicture;
    private int membersCount;
    private List<GroupMemberResponseDto> members;
    private Instant createdAt;
    private Instant updatedAt;
}
