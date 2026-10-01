package com.frigus.coreapi.repository;

import com.frigus.coreapi.enums.ProductListStatus;
import com.frigus.coreapi.model.ShoppingListProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingListProductRepository extends BaseRepository<ShoppingListProduct, Integer> {
    List<ShoppingListProduct> findByListId(UUID listId);

    List<ShoppingListProduct> findByListIdOrderByCreatedAtAsc(UUID listId);

    Page<ShoppingListProduct> findByListId(UUID listId, Pageable pageable);

    Page<ShoppingListProduct> findByListIdAndStatus(UUID listId, ProductListStatus status, Pageable pageable);

    Optional<ShoppingListProduct> findByListIdAndProductId(UUID listId, Integer productId);

    boolean existsByListIdAndProductId(UUID listId, Integer productId);

    long countByListId(UUID listId);

    long countByListIdAndStatus(UUID listId, ProductListStatus status);

    boolean existsByListIdAndStatusNotIn(UUID listId, Collection<ProductListStatus> statuses);

    void deleteByListId(UUID listId);
}
