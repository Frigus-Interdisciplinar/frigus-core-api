package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.recipe.RecipeRequestDto;
import com.frigus.coreapi.dto.recipe.RecipeResponseDto;
import com.frigus.coreapi.model.Recipe;
import org.springframework.stereotype.Component;

@Component
public class RecipeMapper implements BaseMapper<Recipe, RecipeResponseDto, RecipeRequestDto> {
    @Override
    public RecipeResponseDto toDto(Recipe model) {
        return RecipeResponseDto.builder()
                .id(model.getId())
                .name(model.getName())
                .description(model.getDescription())
                .instructions(model.getInstructions())
                .domesticOnly(model.getDomesticOnly())
                .active(model.getActive())
                .createdAt(model.getCreatedAt())
                .build();
    }

    @Override
    public Recipe toEntity(RecipeRequestDto dto) {
        return Recipe.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .instructions(dto.getInstructions())
                .domesticOnly(dto.getDomesticOnly() == null || dto.getDomesticOnly())
                .active(dto.getActive() == null || dto.getActive())
                .build();
    }
}
