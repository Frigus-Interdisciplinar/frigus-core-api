package com.frigus.coreapi.dto.ingredient;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class IngredientRequestDto {
    @NotNull
    private Integer recipeId;

    @NotNull
    private Integer productId;

    @PositiveOrZero
    private BigDecimal quantity;

    private String unit;

    @Builder.Default
    private Boolean required = true;
}
