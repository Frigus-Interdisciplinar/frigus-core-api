package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Recipe;

public interface RecipeRepository extends BaseRepository<Recipe, Integer> {
    org.springframework.data.domain.Page<Recipe> findByActiveTrue(org.springframework.data.domain.Pageable pageable);
    java.util.Optional<Recipe> findByIdAndActiveTrue(Integer id);

 org.springframework.data.domain.Page<Recipe> findByActiveTrueAndNameContainingIgnoreCase(String name,org.springframework.data.domain.Pageable pageable);
 java.util.List<Recipe> findByActiveTrue();
}
