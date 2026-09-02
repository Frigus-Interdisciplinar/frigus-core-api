package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.UserGroup;

import java.util.UUID;

public interface UserGroupRepository extends BaseRepository<UserGroup, Integer> {
    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);
}
