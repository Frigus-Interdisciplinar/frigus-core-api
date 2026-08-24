package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.ShoppingListProduct;

import java.util.List;

public interface ShoppingListProductRepository extends BaseRepository<ShoppingListProduct, Integer> {
    List<ShoppingListProduct> findByListId(java.util.UUID listId);
}
