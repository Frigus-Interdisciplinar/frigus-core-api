package com.frigus.coreapi.repository;

import com.frigus.coreapi.enums.ListStatus;
import com.frigus.coreapi.model.ShoppingList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingListRepository extends BaseRepository<ShoppingList, UUID> {
    Page<ShoppingList> findByStockId(Integer stockId, Pageable pageable);

    Page<ShoppingList> findByStockIdAndStatus(Integer stockId, ListStatus status, Pageable pageable);

    Page<ShoppingList> findByStock_GroupId(UUID groupId, Pageable pageable);

    Page<ShoppingList> findByStock_GroupIdAndStatus(UUID groupId, ListStatus status, Pageable pageable);

    Page<ShoppingList> findByStatus(ListStatus status, Pageable pageable);

    @Query("select l from ShoppingList l where exists (select ug from UserGroup ug " +
            "where ug.group.id = l.stock.group.id and ug.user.id = :userId)")
    Page<ShoppingList> findAccessible(@Param("userId") UUID userId, Pageable pageable);

    @Query("select l from ShoppingList l where l.status = :status and exists (select ug from UserGroup ug " +
            "where ug.group.id = l.stock.group.id and ug.user.id = :userId)")
    Page<ShoppingList> findAccessibleByStatus(@Param("userId") UUID userId,
                                               @Param("status") ListStatus status, Pageable pageable);

    Optional<ShoppingList> findFirstByStockIdAndStatusOrderByCreatedAtDesc(Integer stockId, ListStatus status);
}
