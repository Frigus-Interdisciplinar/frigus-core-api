package com.frigus.coreapi.dto.ingredient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.frigus.coreapi.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IngredientResponseDto {
    private Integer id;
    private Integer recipeId;
    private String recipeName;
    private Integer productId;
    private String productName;
    private Category productCategory;
    private String productUnitOfMeasure;
    private BigDecimal quantity;
    private String unit;
    private Boolean required;
    private Boolean inStock;
    private LocalDate expireDate;
    private Instant createdAt;
    private Instant updatedAt;
}
