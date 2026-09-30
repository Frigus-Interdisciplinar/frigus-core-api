package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.dto.ingredient.IngredientUpdateRequestDto;
import com.frigus.coreapi.service.IngredientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientControllerTest {

    @Mock
    private IngredientService service;

    @InjectMocks
    private IngredientController controller;

    @Test
    @DisplayName("Deve listar ingredientes de forma paginada")
    void shouldFindAllPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        IngredientResponseDto dto = IngredientResponseDto.builder().id(1).build();
        Page<IngredientResponseDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        when(service.findAll(pageable)).thenReturn(page);

        Page<IngredientResponseDto> response = controller.findAll(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(service).findAll(pageable);
    }

    @Test
    @DisplayName("Deve buscar ingrediente por ID")
    void shouldFindById() {
        IngredientResponseDto dto = IngredientResponseDto.builder().id(5).recipeId(1).productId(10).build();
        when(service.findById(5)).thenReturn(dto);

        IngredientResponseDto response = controller.findById(5);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5);
        verify(service).findById(5);
    }

    @Test
    @DisplayName("Deve criar novo ingrediente")
    void shouldCreateIngredient() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(1)
                .productId(10)
                .quantity(new BigDecimal("2.5"))
                .unit("kg")
                .required(true)
                .build();

        IngredientResponseDto expected = IngredientResponseDto.builder()
                .id(1)
                .recipeId(1)
                .productId(10)
                .quantity(new BigDecimal("2.5"))
                .unit("kg")
                .required(true)
                .build();

        when(service.create(request)).thenReturn(expected);

        IngredientResponseDto response = controller.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        verify(service).create(request);
    }

    @Test
    @DisplayName("Deve atualizar ingrediente")
    void shouldUpdateIngredient() {
        IngredientUpdateRequestDto request = IngredientUpdateRequestDto.builder()
                .quantity(new BigDecimal("3.0"))
                .unit("xícara")
                .required(false)
                .build();

        IngredientResponseDto expected = IngredientResponseDto.builder()
                .id(1)
                .quantity(new BigDecimal("3.0"))
                .unit("xícara")
                .required(false)
                .build();

        when(service.update(1, request)).thenReturn(expected);

        IngredientResponseDto response = controller.update(1, request);

        assertThat(response).isNotNull();
        assertThat(response.getQuantity()).isEqualByComparingTo(new BigDecimal("3.0"));
        verify(service).update(1, request);
    }

    @Test
    @DisplayName("Deve listar ingredientes por recipeId")
    void shouldFindByRecipeId() {
        IngredientResponseDto dto = IngredientResponseDto.builder().id(1).recipeId(10).build();
        when(service.findByRecipeId(10)).thenReturn(List.of(dto));

        List<IngredientResponseDto> response = controller.findByRecipeId(10);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRecipeId()).isEqualTo(10);
        verify(service).findByRecipeId(10);
    }

    @Test
    @DisplayName("Deve listar ingredientes por productId")
    void shouldFindByProductId() {
        IngredientResponseDto dto = IngredientResponseDto.builder().id(1).productId(20).build();
        when(service.findByProductId(20)).thenReturn(List.of(dto));

        List<IngredientResponseDto> response = controller.findByProductId(20);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getProductId()).isEqualTo(20);
        verify(service).findByProductId(20);
    }

    @Test
    @DisplayName("Deve deletar ingrediente por ID")
    void shouldDeleteIngredient() {
        controller.delete(1);
        verify(service).delete(1);
    }
}
