package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.dto.ingredient.IngredientUpdateRequestDto;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.IngredientMapper;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.Recipe;
import com.frigus.coreapi.model.RecipeIngredient;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.RecipeIngredientRepository;
import com.frigus.coreapi.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private RecipeIngredientRepository repository;

    @Mock
    private IngredientMapper mapper;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private IngredientService service;

    private Recipe recipe;
    private Product product;
    private RecipeIngredient ingredient;
    private IngredientResponseDto responseDto;

    @BeforeEach
    void setUp() {
        recipe = Recipe.builder()
                .id(1)
                .name("Panqueca")
                .build();

        product = Product.builder()
                .id(10)
                .name("Leite Integral")
                .category(Category.DAIRY)
                .unitOfMeasure(com.frigus.coreapi.enums.UnitOfMeasure.LITER)
                .build();

        ingredient = RecipeIngredient.builder()
                .id(100)
                .recipe(recipe)
                .product(product)
                .quantity(new BigDecimal("0.5"))
                .unit("LITER")
                .required(true)
                .build();

        responseDto = IngredientResponseDto.builder()
                .id(100)
                .recipeId(1)
                .recipeName("Panqueca")
                .productId(10)
                .productName("Leite Integral")
                .quantity(new BigDecimal("0.5"))
                .unit("LITER")
                .required(true)
                .build();
    }

    @Test
    @DisplayName("Deve criar um ingrediente com sucesso informando unidade explícita")
    void shouldCreateIngredientSuccessfullyWithExplicitUnit() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(1)
                .productId(10)
                .quantity(new BigDecimal("250"))
                .unit("ml")
                .required(true)
                .build();

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(repository.existsByRecipeIdAndProductId(1, 10)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(ingredient);
        when(repository.save(any(RecipeIngredient.class))).thenReturn(ingredient);
        when(mapper.toDto(ingredient)).thenReturn(responseDto);

        IngredientResponseDto result = service.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100);
        verify(repository).save(any(RecipeIngredient.class));
    }

    @Test
    @DisplayName("Deve usar unidade de medida do produto quando unidade for omitida")
    void shouldFallbackToProductUnitWhenUnitIsOmitted() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(1)
                .productId(10)
                .quantity(new BigDecimal("1"))
                .unit(null)
                .required(true)
                .build();

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(repository.existsByRecipeIdAndProductId(1, 10)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(ingredient);
        when(repository.save(any(RecipeIngredient.class))).thenAnswer(invocation -> {
            RecipeIngredient saved = invocation.getArgument(0);
            assertThat(saved.getUnit()).isEqualTo("LITER");
            return saved;
        });
        when(mapper.toDto(any())).thenReturn(responseDto);

        IngredientResponseDto result = service.create(request);

        assertThat(result).isNotNull();
        verify(repository).save(any(RecipeIngredient.class));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao criar ingrediente com receita inexistente")
    void shouldThrowNotFoundWhenCreatingWithNonExistentRecipe() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(999)
                .productId(10)
                .build();

        when(recipeRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Receita não encontrada");
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao criar ingrediente com produto inexistente")
    void shouldThrowNotFoundWhenCreatingWithNonExistentProduct() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(1)
                .productId(999)
                .build();

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Produto não encontrado");
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao tentar adicionar produto já existente na receita")
    void shouldThrowConflictWhenIngredientAlreadyExistsInRecipe() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(1)
                .productId(10)
                .build();

        when(recipeRepository.findById(1)).thenReturn(Optional.of(recipe));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(repository.existsByRecipeIdAndProductId(1, 10)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ingrediente já cadastrado");
    }

    @Test
    @DisplayName("Deve atualizar campos de um ingrediente com IngredientUpdateRequestDto")
    void shouldUpdateIngredientFields() {
        IngredientUpdateRequestDto updateDto = IngredientUpdateRequestDto.builder()
                .quantity(new BigDecimal("2.0"))
                .unit("xícara")
                .required(false)
                .build();

        when(repository.findById(100)).thenReturn(Optional.of(ingredient));
        when(repository.save(ingredient)).thenReturn(ingredient);
        when(mapper.toDto(ingredient)).thenReturn(responseDto);

        IngredientResponseDto result = service.update(100, updateDto);

        assertThat(result).isNotNull();
        assertThat(ingredient.getQuantity()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(ingredient.getUnit()).isEqualTo("xícara");
        assertThat(ingredient.getRequired()).isFalse();
    }

    @Test
    @DisplayName("Deve atualizar produto de um ingrediente validando unicidade")
    void shouldUpdateIngredientProduct() {
        Product newProduct = Product.builder().id(20).name("Farinha").build();
        IngredientUpdateRequestDto updateDto = IngredientUpdateRequestDto.builder()
                .productId(20)
                .build();

        when(repository.findById(100)).thenReturn(Optional.of(ingredient));
        when(productRepository.findById(20)).thenReturn(Optional.of(newProduct));
        when(repository.existsByRecipeIdAndProductId(1, 20)).thenReturn(false);
        when(repository.save(ingredient)).thenReturn(ingredient);
        when(mapper.toDto(ingredient)).thenReturn(responseDto);

        service.update(100, updateDto);

        assertThat(ingredient.getProduct()).isEqualTo(newProduct);
    }

    @Test
    @DisplayName("Deve lançar ConflictException ao alterar produto para um já existente na receita")
    void shouldThrowConflictWhenUpdatingProductToExistingOne() {
        Product newProduct = Product.builder().id(20).name("Farinha").build();
        IngredientUpdateRequestDto updateDto = IngredientUpdateRequestDto.builder()
                .productId(20)
                .build();

        when(repository.findById(100)).thenReturn(Optional.of(ingredient));
        when(productRepository.findById(20)).thenReturn(Optional.of(newProduct));
        when(repository.existsByRecipeIdAndProductId(1, 20)).thenReturn(true);

        assertThatThrownBy(() -> service.update(100, updateDto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ingrediente já cadastrado");
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao atualizar ingrediente inexistente")
    void shouldThrowNotFoundWhenUpdatingNonExistentIngredient() {
        IngredientUpdateRequestDto updateDto = IngredientUpdateRequestDto.builder().build();
        when(repository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999, updateDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Ingrediente não encontrado");
    }

    @Test
    @DisplayName("Deve atualizar com sucesso usando overload IngredientRequestDto")
    void shouldUpdateWithIngredientRequestDto() {
        IngredientRequestDto request = IngredientRequestDto.builder()
                .recipeId(1)
                .productId(10)
                .quantity(new BigDecimal("500"))
                .unit("g")
                .required(true)
                .build();

        when(repository.findById(100)).thenReturn(Optional.of(ingredient));
        when(repository.save(ingredient)).thenReturn(ingredient);
        when(mapper.toDto(ingredient)).thenReturn(responseDto);

        IngredientResponseDto result = service.update(100, request);

        assertThat(result).isNotNull();
        assertThat(ingredient.getQuantity()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(ingredient.getUnit()).isEqualTo("g");
    }

    @Test
    @DisplayName("Deve deletar ingrediente existente")
    void shouldDeleteIngredientSuccessfully() {
        when(repository.findById(100)).thenReturn(Optional.of(ingredient));

        service.delete(100);

        verify(repository).delete(ingredient);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao deletar ingrediente inexistente")
    void shouldThrowNotFoundWhenDeletingNonExistentIngredient() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Ingrediente não encontrado");
    }

    @Test
    @DisplayName("Deve buscar ingredientes por recipeId com sucesso")
    void shouldFindByRecipeId() {
        when(recipeRepository.existsById(1)).thenReturn(true);
        when(repository.findByRecipeId(1)).thenReturn(List.of(ingredient));
        when(mapper.toDto(ingredient)).thenReturn(responseDto);

        List<IngredientResponseDto> result = service.findByRecipeId(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecipeId()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar ingredientes de receita inexistente")
    void shouldThrowNotFoundWhenFindingByNonExistentRecipe() {
        when(recipeRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> service.findByRecipeId(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Receita não encontrada");
    }

    @Test
    @DisplayName("Deve buscar ingredientes por productId com sucesso")
    void shouldFindByProductId() {
        when(productRepository.existsById(10)).thenReturn(true);
        when(repository.findByProductId(10)).thenReturn(List.of(ingredient));
        when(mapper.toDto(ingredient)).thenReturn(responseDto);

        List<IngredientResponseDto> result = service.findByProductId(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(10);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar ingredientes de produto inexistente")
    void shouldThrowNotFoundWhenFindingByNonExistentProduct() {
        when(productRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> service.findByProductId(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Produto não encontrado");
    }

    @Test
    @DisplayName("Deve deletar todos os ingredientes de uma receita com deleteByRecipeId")
    void shouldDeleteAllIngredientsByRecipeId() {
        when(recipeRepository.existsById(1)).thenReturn(true);

        service.deleteByRecipeId(1);

        verify(repository).deleteByRecipeId(1);
    }

    @Test
    @DisplayName("Deve substituir todos os ingredientes de uma receita com replaceAll")
    void shouldReplaceAllIngredientsForRecipe() {
        IngredientRequestDto item = IngredientRequestDto.builder()
                .productId(10)
                .quantity(new BigDecimal("100"))
                .unit("ml")
                .required(true)
                .build();

        when(productRepository.findById(10)).thenReturn(Optional.of(product));

        service.replaceAll(recipe, List.of(item));

        verify(repository).deleteByRecipeId(recipe.getId());
        verify(repository).save(any(RecipeIngredient.class));
    }
}
