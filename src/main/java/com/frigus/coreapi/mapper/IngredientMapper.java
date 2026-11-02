package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.model.RecipeIngredient;
import org.springframework.stereotype.Component;

@Component
public class IngredientMapper implements BaseMapper<RecipeIngredient, IngredientResponseDto, IngredientRequestDto> {

    @Override
    public IngredientResponseDto toDto(RecipeIngredient model) {
        if (model == null) {
            return null;
        }
        return IngredientResponseDto.builder()
                .id(model.getId())
                .recipeId(model.getRecipe() != null ? model.getRecipe().getId() : null)
                .recipeName(model.getRecipe() != null ? model.getRecipe().getName() : null)
                .productId(model.getProduct() != null ? model.getProduct().getId() : null)
                .productName(model.getProduct() != null ? model.getProduct().getName() : null)
                .productCategory(model.getProduct() != null ? model.getProduct().getCategory() : null)
                .productUnitOfMeasure(model.getProduct() != null && model.getProduct().getUnitOfMeasure() != null ? model.getProduct().getUnitOfMeasure().name() : null)
                .quantity(model.getQuantity())
                .unit(model.getUnit())
                .required(model.getRequired())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    @Override
    public RecipeIngredient toEntity(IngredientRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return RecipeIngredient.builder()
                .quantity(dto.getQuantity())
                .unit(dto.getUnit())
                .required(dto.getRequired() == null || dto.getRequired())
                .build();
    }
}
