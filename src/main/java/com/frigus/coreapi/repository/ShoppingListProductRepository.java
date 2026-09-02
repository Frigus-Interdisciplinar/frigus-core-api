package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.ShoppingListProduct;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingListProductRepository extends BaseRepository<ShoppingListProduct, Integer> {
}
