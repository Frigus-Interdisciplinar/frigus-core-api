package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.Recipe;
import com.frigus.coreapi.model.RecipeIngredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientMapperTest {

    private final IngredientMapper mapper = new IngredientMapper();

    @Test
    @DisplayName("Deve mapear entidade RecipeIngredient para IngredientResponseDto com todos os campos")
    void shouldMapEntityToResponseDto() {
        Instant now = Instant.now();
        Recipe recipe = Recipe.builder()
                .id(10)
                .name("Bolo de Cenoura")
                .build();

        Product product = Product.builder()
                .id(25)
                .name("Farinha de Trigo")
                .category(Category.GRAIN)
                .unitOfMeasure(com.frigus.coreapi.enums.UnitOfMeasure.KILOGRAM)
                .build();

        RecipeIngredient model = RecipeIngredient.builder()
                .id(1)
                .recipe(recipe)
                .product(product)
                .quantity(new BigDecimal("2.50"))
                .unit("xícara")
                .required(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        IngredientResponseDto dto = mapper.toDto(model);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getRecipeId()).isEqualTo(10);
        assertThat(dto.getRecipeName()).isEqualTo("Bolo de Cenoura");
        assertThat(dto.getProductId()).isEqualTo(25);
        assertThat(dto.getProductName()).isEqualTo("Farinha de Trigo");
        assertThat(dto.getProductCategory()).isEqualTo(Category.GRAIN);
        assertThat(dto.getProductUnitOfMeasure()).isEqualTo("KILOGRAM");
        assertThat(dto.getQuantity()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(dto.getUnit()).isEqualTo("xícara");
        assertThat(dto.getRequired()).isTrue();
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve retornar null ao mapear entidade nula para DTO")
    void shouldReturnNullWhenEntityIsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("Deve lidar com relacionamentos nulos no mapeamento para DTO")
    void shouldHandleNullRelationsWhenMappingToDto() {
        RecipeIngredient model = RecipeIngredient.builder()
                .id(2)
                .quantity(new BigDecimal("100"))
                .required(false)
                .build();

        IngredientResponseDto dto = mapper.toDto(model);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(2);
        assertThat(dto.getRecipeId()).isNull();
        assertThat(dto.getRecipeName()).isNull();
        assertThat(dto.getProductId()).isNull();
        assertThat(dto.getProductName()).isNull();
        assertThat(dto.getProductCategory()).isNull();
    }

    @Test
    @DisplayName("Deve mapear IngredientRequestDto para entidade RecipeIngredient")
    void shouldMapRequestDtoToEntity() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(10)
                .productId(25)
                .quantity(new BigDecimal("300"))
                .unit("ml")
                .required(true)
                .build();

        RecipeIngredient entity = mapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getQuantity()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(entity.getUnit()).isEqualTo("ml");
        assertThat(entity.getRequired()).isTrue();
        assertThat(entity.getId()).isNull();
    }

    @Test
    @DisplayName("Deve retornar null ao mapear request DTO nulo para entidade")
    void shouldReturnNullWhenRequestDtoIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("Deve assumir required como true se omitido no request DTO")
    void shouldDefaultRequiredToTrueWhenOmitted() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(1)
                .productId(2)
                .quantity(BigDecimal.ONE)
                .required(null)
                .build();

        RecipeIngredient entity = mapper.toEntity(request);

        assertThat(entity.getRequired()).isTrue();
    }
}
