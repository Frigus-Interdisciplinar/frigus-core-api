package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.group.GroupCreateRequestDto;
import com.frigus.coreapi.dto.group.GroupMemberResponseDto;
import com.frigus.coreapi.dto.group.GroupResponseDto;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.UserGroup;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroupMapper implements BaseMapper<Group, GroupResponseDto, GroupCreateRequestDto> {

    @Override
    public GroupResponseDto toDto(Group group) {
        if (group == null) return null;
        return GroupResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .bannerPicture(group.getBannerPicture())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public GroupResponseDto toDtoWithMembers(Group group, List<UserGroup> userGroups) {
        if (group == null) return null;
        List<GroupMemberResponseDto> memberDtos = userGroups != null ? userGroups.stream()
                .map(this::toMemberDto)
                .toList() : List.of();

        return GroupResponseDto.builder()
                .id(group.getId())
                .name(group.getName())
                .bannerPicture(group.getBannerPicture())
                .membersCount(memberDtos.size())
                .members(memberDtos)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public GroupMemberResponseDto toMemberDto(UserGroup userGroup) {
        if (userGroup == null || userGroup.getUser() == null) return null;
        return GroupMemberResponseDto.builder()
                .userId(userGroup.getUser().getId())
                .name(userGroup.getUser().getName())
                .email(userGroup.getUser().getEmail())
                .joinedAt(userGroup.getCreatedAt())
                .build();
    }

    @Override
    public Group toEntity(GroupCreateRequestDto dto) {
        if (dto == null) return null;
        return Group.builder()
                .name(dto.getName())
                .bannerPicture(dto.getBannerPicture())
                .build();
    }
}
