package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends BaseRepository<Product, Integer> {
    Optional<Product> findFirstByNameIgnoreCase(String name);

    Optional<Product> findFirstByNameIgnoreCaseAndOwnerGroupId(String name, UUID ownerGroupId);

    @Query("SELECT p FROM Product p WHERE p.ownerGroupId IS NULL OR p.ownerGroupId = :groupId")
    Page<Product> findAllGlobalOrByGroupId(@Param("groupId") UUID groupId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.ownerGroupId IS NULL")
    Page<Product> findAllGlobal(Pageable pageable);

    @Query(value = """
            select exists (
                select 1 from stock_products where product_id = :productId
                union all
                select 1 from recipe_ingredients where product_id = :productId
                union all
                select 1 from shopping_list_products where product_id = :productId
                union all
                select 1 from requests where product_id = :productId
            )
            """, nativeQuery = true)
    boolean isReferenced(@Param("productId") Integer productId);
}
