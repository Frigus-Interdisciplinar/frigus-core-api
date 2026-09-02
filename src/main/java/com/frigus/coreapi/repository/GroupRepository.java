package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends BaseRepository<Group, UUID> {
    Optional<Group> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT ug.group FROM UserGroup ug WHERE ug.user.id = :userId AND ug.group.deletedAt IS NULL")
    List<Group> findGroupsByUserId(@Param("userId") UUID userId);

    @Query("SELECT ug.group FROM UserGroup ug WHERE ug.user.id = :userId AND ug.group.deletedAt IS NULL")
    Page<Group> findGroupsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
