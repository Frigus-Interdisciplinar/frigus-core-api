package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.group.GroupCreateRequestDto;
import com.frigus.coreapi.dto.group.GroupMemberResponseDto;
import com.frigus.coreapi.dto.group.GroupResponseDto;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.model.UserGroup;
import com.frigus.coreapi.utils.ServiceUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GroupMapper implements BaseMapper<Group, GroupResponseDto, GroupCreateRequestDto> {

    @Override
    public GroupResponseDto toDto(Group group) {
        if (group == null) return null;
        User currentUser = ServiceUtils.getCurrentUser();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;
        UUID ownerId = group.getOwner() != null ? group.getOwner().getId() : null;
        String ownerName = group.getOwner() != null ? group.getOwner().getName() : null;

        return GroupResponseDto.builder()
                .id(group.getId())
                .ownerId(ownerId)
                .ownerName(ownerName)
                .isOwner(ownerId != null && ownerId.equals(currentUserId))
                .name(group.getName())
                .bannerPicture(group.getBannerPicture())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public GroupResponseDto toDtoWithMembers(Group group, List<UserGroup> userGroups) {
        return toDtoWithMembers(group, userGroups, null);
    }

    public GroupResponseDto toDtoWithMembers(Group group, List<UserGroup> userGroups, UUID defaultConversationId) {
        if (group == null) return null;
        User currentUser = ServiceUtils.getCurrentUser();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;
        UUID ownerId = group.getOwner() != null ? group.getOwner().getId() : null;
        String ownerName = group.getOwner() != null ? group.getOwner().getName() : null;

        List<GroupMemberResponseDto> memberDtos = userGroups != null ? userGroups.stream()
                .map(ug -> toMemberDto(ug, ownerId))
                .toList() : List.of();

        return GroupResponseDto.builder()
                .id(group.getId())
                .ownerId(ownerId)
                .ownerName(ownerName)
                .isOwner(ownerId != null && ownerId.equals(currentUserId))
                .defaultConversationId(defaultConversationId)
                .name(group.getName())
                .bannerPicture(group.getBannerPicture())
                .membersCount(memberDtos.size())
                .members(memberDtos)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    public GroupMemberResponseDto toMemberDto(UserGroup userGroup) {
        UUID ownerId = userGroup != null && userGroup.getGroup() != null && userGroup.getGroup().getOwner() != null 
                ? userGroup.getGroup().getOwner().getId() 
                : null;
        return toMemberDto(userGroup, ownerId);
    }

    public GroupMemberResponseDto toMemberDto(UserGroup userGroup, UUID ownerId) {
        if (userGroup == null || userGroup.getUser() == null) return null;
        boolean isOwner = ownerId != null && ownerId.equals(userGroup.getUser().getId());
        return GroupMemberResponseDto.builder()
                .userId(userGroup.getUser().getId())
                .name(userGroup.getUser().getName())
                .email(userGroup.getUser().getEmail())
                .isOwner(isOwner)
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
