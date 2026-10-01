package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.recipe.RecipeRequestDto;
import com.frigus.coreapi.dto.recipe.RecipeResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.RecipeMapper;
import com.frigus.coreapi.model.Recipe;
import com.frigus.coreapi.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RecipeService extends BaseService<Recipe, Integer, RecipeRequestDto, RecipeResponseDto, RecipeMapper, RecipeRepository> {
    public RecipeService(RecipeRepository repository, RecipeMapper mapper) {
        super(repository, mapper);
    }

    public RecipeResponseDto create(RecipeRequestDto dto) {
        Recipe recipe = mapper.toEntity(dto);
        recipe.setCreatedAt(Instant.now());
        return mapper.toDto(repository.save(recipe));
    }

    public RecipeResponseDto update(Integer id, RecipeRequestDto dto) {
        Recipe recipe = repository.findById(id)
                .orElseThrow(NotFoundException::new);
        recipe.setName(dto.getName());
        recipe.setDescription(dto.getDescription());
        recipe.setInstructions(dto.getInstructions());
        recipe.setDomesticOnly(dto.getDomesticOnly() == null || dto.getDomesticOnly());
        recipe.setActive(dto.getActive() == null || dto.getActive());
        return mapper.toDto(repository.save(recipe));
    }

    public void deleteRecipe(Integer id) {
        Recipe recipe = repository.findById(id)
                .orElseThrow(NotFoundException::new);
        recipe.setActive(false);
        repository.save(recipe);
    }
}
