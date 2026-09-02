package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.UserGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends BaseRepository<UserGroup, Integer> {
    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);

    Optional<UserGroup> findByUserIdAndGroupId(UUID userId, UUID groupId);

    List<UserGroup> findByGroupId(UUID groupId);

    int countByGroupId(UUID groupId);

    void deleteByUserIdAndGroupId(UUID userId, UUID groupId);

    @Query("SELECT CASE WHEN COUNT(ug1) > 0 THEN true ELSE false END " +
           "FROM UserGroup ug1, UserGroup ug2 " +
           "WHERE ug1.user.id = :userA " +
           "AND ug2.user.id = :userB " +
           "AND ug1.group.id = ug2.group.id " +
           "AND ug1.group.deletedAt IS NULL")
    boolean existsCommonGroupByUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
