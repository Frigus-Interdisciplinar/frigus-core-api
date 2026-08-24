package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.IngredientMapper;
import com.frigus.coreapi.model.RecipeIngredient;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.RecipeIngredientRepository;
import com.frigus.coreapi.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService extends BaseService<RecipeIngredient, Integer, IngredientRequestDto, IngredientResponseDto, IngredientMapper, RecipeIngredientRepository> {
    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;

    public IngredientService(
            RecipeIngredientRepository repository,
            IngredientMapper mapper,
            RecipeRepository recipeRepository,
            ProductRepository productRepository) {
        super(repository, mapper);
        this.recipeRepository = recipeRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public IngredientResponseDto create(IngredientRequestDto dto) {
        RecipeIngredient ingredient = mapper.toEntity(dto);
        ingredient.setRecipe(recipeRepository.findById(dto.getRecipeId()).orElseThrow(NotFoundException::new));
        ingredient.setProduct(productRepository.findById(dto.getProductId()).orElseThrow(NotFoundException::new));
        return mapper.toDto(repository.save(ingredient));
    }

    @Transactional
    public IngredientResponseDto update(Integer id, IngredientRequestDto dto) {
        RecipeIngredient ingredient = repository.findById(id).orElseThrow(NotFoundException::new);
        ingredient.setRecipe(recipeRepository.findById(dto.getRecipeId()).orElseThrow(NotFoundException::new));
        ingredient.setProduct(productRepository.findById(dto.getProductId()).orElseThrow(NotFoundException::new));
        ingredient.setQuantity(dto.getQuantity());
        ingredient.setUnit(dto.getUnit());
        ingredient.setRequired(dto.getRequired() == null || dto.getRequired());
        return mapper.toDto(repository.save(ingredient));
    }

    @Transactional(readOnly = true)
    public java.util.List<IngredientResponseDto> findByRecipeId(Integer recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new NotFoundException();
        }
        return repository.findByRecipeId(recipeId).stream().map(mapper::toDto).toList();
    }
}
