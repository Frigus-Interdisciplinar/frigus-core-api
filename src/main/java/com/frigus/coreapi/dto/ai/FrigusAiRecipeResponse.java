package com.frigus.coreapi.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FrigusAiRecipeResponse {
    private String sessionId;
    private String chatMessage;
    private String recipeName;
    private String description;
    private String instructions;
    private List<AiRecipeIngredientPayload> ingredients;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRecipeIngredientPayload {
        private Integer productId;
        private String productName;
        private BigDecimal quantity;
        private String unit;
        private Boolean required;
    }
}
