package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.RecipeSuggestion;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeSuggestionRepository extends BaseRepository<RecipeSuggestion, Integer> {
    List<RecipeSuggestion> findByStockIdOrderByScoreDesc(Integer stockId);
    List<RecipeSuggestion> findByRecipeId(Integer recipeId);
    java.util.Optional<RecipeSuggestion> findFirstByStockIdOrderByIdDesc(Integer stockId);
    List<RecipeSuggestion> findTop5ByStockIdOrderByIdDesc(Integer stockId);
}
