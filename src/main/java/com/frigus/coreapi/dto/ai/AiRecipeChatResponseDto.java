package com.frigus.coreapi.dto.ai;

import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiRecipeChatResponseDto {
    private String sessionId;
    private String chatMessage;

    private Integer recipeId;
    private String recipeName;
    private String recipeDescription;
    private String recipeInstructions;
    private List<IngredientResponseDto> ingredients;

    private Integer suggestionId;
    private Integer matchedIngredients;
    private Integer missingIngredients;
    private BigDecimal score;
    private LocalDate nearestExpireDate;
}
