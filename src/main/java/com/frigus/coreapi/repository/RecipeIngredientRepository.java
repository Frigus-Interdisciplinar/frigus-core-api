package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.RecipeIngredient;

import java.util.List;
import java.util.Optional;

public interface RecipeIngredientRepository extends BaseRepository<RecipeIngredient, Integer> {
    List<RecipeIngredient> findByRecipeId(Integer recipeId);
    List<RecipeIngredient> findByProductId(Integer productId);
    Optional<RecipeIngredient> findByRecipeIdAndProductId(Integer recipeId, Integer productId);
    boolean existsByRecipeIdAndProductId(Integer recipeId, Integer productId);
    void deleteByRecipeId(Integer recipeId);
}
