package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.RecipeIngredient;

import java.util.List;

public interface RecipeIngredientRepository extends BaseRepository<RecipeIngredient, Integer> {
    List<RecipeIngredient> findByRecipeId(Integer recipeId);
}
