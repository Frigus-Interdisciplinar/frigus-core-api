package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends BaseRepository<Product, Integer> {
    Optional<Product> findFirstByNameIgnoreCase(String name);
    Optional<Product> findFirstByNameIgnoreCaseAndOwnerGroupIsNull(String name);

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
    org.springframework.data.domain.Page<Product> findByOwnerGroupIsNull(org.springframework.data.domain.Pageable pageable);
    java.util.List<Product> findByOwnerGroupId(java.util.UUID groupId);
    java.util.Optional<Product> findFirstByOwnerGroupIdAndNameIgnoreCase(java.util.UUID groupId,String name);
}
