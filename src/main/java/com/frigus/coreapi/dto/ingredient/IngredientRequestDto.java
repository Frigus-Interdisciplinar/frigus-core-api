package com.frigus.coreapi.dto.ingredient;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @NotNull(message = "O ID da receita é obrigatório")
    private Integer recipeId;

    @NotNull(message = "O ID do produto é obrigatório")
    private Integer productId;

    @Positive(message = "A quantidade deve ser maior que zero")
    private BigDecimal quantity;

    private String unit;

    @Builder.Default
    private Boolean required = true;
}
