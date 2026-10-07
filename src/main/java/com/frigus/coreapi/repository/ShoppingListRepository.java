package com.frigus.coreapi.repository;

import com.frigus.coreapi.enums.ListStatus;
import com.frigus.coreapi.model.ShoppingList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingListRepository extends BaseRepository<ShoppingList, UUID> {
    Page<ShoppingList> findByStockId(Integer stockId, Pageable pageable);

    Page<ShoppingList> findByStockIdAndStatus(Integer stockId, ListStatus status, Pageable pageable);

    Page<ShoppingList> findByStock_GroupId(UUID groupId, Pageable pageable);

    Page<ShoppingList> findByStock_GroupIdAndStatus(UUID groupId, ListStatus status, Pageable pageable);

    Optional<ShoppingList> findFirstByStockIdAndStatusOrderByCreatedAtDesc(Integer stockId, ListStatus status);
}
