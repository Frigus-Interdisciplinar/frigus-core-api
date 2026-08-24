package com.frigus.coreapi.dto.ingredient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IngredientResponseDto {
    private Integer id;
    private Integer recipeId;
    private Integer productId;
    private BigDecimal quantity;
    private String unit;
    private Boolean required;
}
