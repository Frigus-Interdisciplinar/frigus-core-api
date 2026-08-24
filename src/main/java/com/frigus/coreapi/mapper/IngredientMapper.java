package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.model.RecipeIngredient;
import org.springframework.stereotype.Component;

@Component
public class IngredientMapper implements BaseMapper<RecipeIngredient, IngredientResponseDto, IngredientRequestDto> {
    @Override
    public IngredientResponseDto toDto(RecipeIngredient model) {
        return IngredientResponseDto.builder()
                .id(model.getId())
                .recipeId(model.getRecipe().getId())
                .productId(model.getProduct().getId())
                .quantity(model.getQuantity())
                .unit(model.getUnit())
                .required(model.getRequired())
                .build();
    }

    @Override
    public RecipeIngredient toEntity(IngredientRequestDto dto) {
        return RecipeIngredient.builder()
                .quantity(dto.getQuantity())
                .unit(dto.getUnit())
                .required(dto.getRequired() == null || dto.getRequired())
                .build();
    }
}
