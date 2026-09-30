package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends BaseRepository<Product, Integer> {
    Optional<Product> findFirstByNameIgnoreCase(String name);

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
