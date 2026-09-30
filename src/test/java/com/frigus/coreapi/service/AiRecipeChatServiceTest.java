package com.frigus.coreapi.service;

import com.frigus.coreapi.client.FrigusAiClient;
import com.frigus.coreapi.dto.ai.AiRecipeChatRequestDto;
import com.frigus.coreapi.dto.ai.AiRecipeChatResponseDto;
import com.frigus.coreapi.dto.ai.FrigusAiPromptPayload;
import com.frigus.coreapi.dto.ai.FrigusAiRecipeResponse;
import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.ProductStatus;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.RecipeRepository;
import com.frigus.coreapi.repository.RecipeSuggestionRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import com.frigus.coreapi.repository.StockRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiRecipeChatServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockProductRepository stockProductRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientService ingredientService;

    @Mock
    private RecipeSuggestionRepository recipeSuggestionRepository;

    @Mock
    private FrigusAiClient frigusAiClient;

    @Mock
    private UserGroupRepository userGroupRepository;

    @InjectMocks
    private AiRecipeChatService service;

    private User user;
    private Group group;
    private Stock stock;
    private Product chickenProduct;
    private Product tomatoProduct;
    private StockProduct stockProduct1;
    private StockProduct stockProduct2;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("test@frigus.com").build();
        group = Group.builder().id(UUID.randomUUID()).name("Família Teste").build();
        stock = Stock.builder().id(1).name("Geladeira Principal").group(group).build();

        chickenProduct = Product.builder()
                .id(10)
                .name("Peito de Frango")
                .category(Category.MEAT)
                .unitOfMeasure(com.frigus.coreapi.enums.UnitOfMeasure.KILOGRAM)
                .build();

        tomatoProduct = Product.builder()
                .id(20)
                .name("Tomate")
                .category(Category.VEGETABLE)
                .unitOfMeasure(com.frigus.coreapi.enums.UnitOfMeasure.UNIT)
                .build();

        stockProduct1 = StockProduct.builder()
                .id(100)
                .stock(stock)
                .product(chickenProduct)
                .quantity(1)
                .expireDate(LocalDate.now().plusDays(2))
                .productStatus(ProductStatus.NEAR_EXPIRATION)
                .build();

        stockProduct2 = StockProduct.builder()
                .id(101)
                .stock(stock)
                .product(tomatoProduct)
                .quantity(3)
                .expireDate(LocalDate.now().plusDays(5))
                .productStatus(ProductStatus.FRESH)
                .build();
    }

    @Test
    @DisplayName("Deve processar mensagem do usuário e gerar receita com IA salvando ingredientes e sugestão")
    void shouldProcessUserMessageAndSaveRecipeWithIngredients() {
        AiRecipeChatRequestDto request = AiRecipeChatRequestDto.builder()
                .message("O que fazer para o jantar?")
                .stockId(1)
                .sessionId("session-123")
                .build();

        FrigusAiRecipeResponse aiResponse = FrigusAiRecipeResponse.builder()
                .sessionId("session-123")
                .chatMessage("Preparei um frango refogado com tomate!")
                .recipeName("Frango Refogado com Tomate")
                .description("Refogado simples e saboroso.")
                .instructions("1. Pique o frango...\n2. Refogue com o tomate.")
                .ingredients(List.of(
                        FrigusAiRecipeResponse.AiRecipeIngredientPayload.builder()
                                .productId(10)
                                .productName("Peito de Frango")
                                .quantity(new BigDecimal("0.5"))
                                .unit("kg")
                                .required(true)
                                .build(),
                        FrigusAiRecipeResponse.AiRecipeIngredientPayload.builder()
                                .productId(20)
                                .productName("Tomate")
                                .quantity(new BigDecimal("2"))
                                .unit("unidade")
                                .required(true)
                                .build()
                ))
                .build();

        Recipe savedRecipe = Recipe.builder()
                .id(42)
                .name("Frango Refogado com Tomate")
                .description("Refogado simples e saboroso.")
                .instructions("1. Pique o frango...\n2. Refogue com o tomate.")
                .build();

        IngredientResponseDto ing1 = IngredientResponseDto.builder()
                .id(1)
                .recipeId(42)
                .productId(10)
                .productName("Peito de Frango")
                .build();

        IngredientResponseDto ing2 = IngredientResponseDto.builder()
                .id(2)
                .recipeId(42)
                .productId(20)
                .productName("Tomate")
                .build();

        RecipeSuggestion savedSuggestion = RecipeSuggestion.builder()
                .id(88)
                .recipe(savedRecipe)
                .stock(stock)
                .matchedIngredients(2)
                .missingIngredients(0)
                .score(BigDecimal.ONE)
                .nearestExpireDate(stockProduct1.getExpireDate())
                .build();

        when(stockRepository.findById(1)).thenReturn(Optional.of(stock));
        when(userGroupRepository.existsByUserIdAndGroupId(user.getId(), group.getId())).thenReturn(true);
        when(stockProductRepository.findByStockId(1)).thenReturn(List.of(stockProduct1, stockProduct2));
        when(frigusAiClient.requestRecipeFromAi(any())).thenReturn(aiResponse);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(savedRecipe);
        when(ingredientService.create(any(IngredientRequestDto.class))).thenReturn(ing1, ing2);
        when(recipeSuggestionRepository.save(any(RecipeSuggestion.class))).thenReturn(savedSuggestion);

        AiRecipeChatResponseDto response = service.processUserMessage(user, request);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("session-123");
        assertThat(response.getRecipeId()).isEqualTo(42);
        assertThat(response.getRecipeName()).isEqualTo("Frango Refogado com Tomate");
        assertThat(response.getIngredients()).hasSize(2);
        assertThat(response.getMatchedIngredients()).isEqualTo(2);
        assertThat(response.getMissingIngredients()).isZero();
        assertThat(response.getNearestExpireDate()).isEqualTo(stockProduct1.getExpireDate());

        ArgumentCaptor<FrigusAiPromptPayload> payloadCaptor = ArgumentCaptor.forClass(FrigusAiPromptPayload.class);
        verify(frigusAiClient).requestRecipeFromAi(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getUserId()).isEqualTo(user.getId().toString());

        verify(recipeRepository).save(any(Recipe.class));
        verify(ingredientService, times(2)).create(any(IngredientRequestDto.class));
        verify(recipeSuggestionRepository).save(any(RecipeSuggestion.class));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando usuário não pertence ao grupo do estoque")
    void shouldThrowForbiddenWhenUserDoesNotBelongToStockGroup() {
        AiRecipeChatRequestDto request = AiRecipeChatRequestDto.builder()
                .message("Receita")
                .stockId(1)
                .build();

        when(stockRepository.findById(1)).thenReturn(Optional.of(stock));
        when(userGroupRepository.existsByUserIdAndGroupId(user.getId(), group.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.processUserMessage(user, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Acesso negado");
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando estoque não for encontrado")
    void shouldThrowNotFoundWhenStockDoesNotExist() {
        AiRecipeChatRequestDto request = AiRecipeChatRequestDto.builder()
                .message("Receita")
                .stockId(999)
                .build();

        when(stockRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processUserMessage(user, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Estoque não encontrado");
    }

    @Test
    @DisplayName("Deve listar sugestões salvas para um estoque quando usuário tem acesso")
    void shouldGetSuggestionsByStock() {
        Recipe recipe = Recipe.builder().id(42).name("Omelete").build();
        RecipeSuggestion suggestion = RecipeSuggestion.builder()
                .id(1)
                .recipe(recipe)
                .stock(stock)
                .matchedIngredients(2)
                .missingIngredients(1)
                .score(new BigDecimal("0.67"))
                .nearestExpireDate(LocalDate.now().plusDays(1))
                .build();

        when(stockRepository.findById(1)).thenReturn(Optional.of(stock));
        when(userGroupRepository.existsByUserIdAndGroupId(user.getId(), group.getId())).thenReturn(true);
        when(recipeSuggestionRepository.findByStockIdOrderByScoreDesc(1)).thenReturn(List.of(suggestion));
        when(ingredientService.findByRecipeId(42)).thenReturn(List.of());

        List<AiRecipeChatResponseDto> result = service.getSuggestionsByStock(user, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecipeName()).isEqualTo("Omelete");
        assertThat(result.get(0).getMatchedIngredients()).isEqualTo(2);
        assertThat(result.get(0).getMissingIngredients()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException ao buscar sugestões se usuário não pertencer ao grupo")
    void shouldThrowForbiddenWhenGettingSuggestionsAndUserDoesNotBelongToStockGroup() {
        when(stockRepository.findById(1)).thenReturn(Optional.of(stock));
        when(userGroupRepository.existsByUserIdAndGroupId(user.getId(), group.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.getSuggestionsByStock(user, 1))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Acesso negado");
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar sugestões de estoque inexistente")
    void shouldThrowNotFoundWhenGettingSuggestionsForNonExistentStock() {
        when(stockRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSuggestionsByStock(user, 999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Estoque não encontrado");
    }

    @Test
    @DisplayName("Deve processar mensagem e utilizar sugestão da IA gravada no banco com validade nos ingredientes")
    void shouldProcessUserMessageUsingAiSuggestionFromDatabase() {
        AiRecipeChatRequestDto requestDto = AiRecipeChatRequestDto.builder()
                .stockId(1)
                .message("O que posso fazer com meu estoque?")
                .build();

        FrigusAiRecipeResponse aiResponse = FrigusAiRecipeResponse.builder()
                .sessionId("ai-session-123")
                .chatMessage("Encontrei ótimas receitas no seu estoque!")
                .build(); // recipeName is null, indicating AI chat response with database suggestions

        Recipe existingRecipe = Recipe.builder()
                .id(10)
                .name("Refeição Prática")
                .description("Receita deliciosa.")
                .instructions("1. Prepare os ingredientes.")
                .build();

        RecipeSuggestion aiSuggestion = RecipeSuggestion.builder()
                .id(999)
                .recipe(existingRecipe)
                .stock(stock)
                .matchedIngredients(2)
                .missingIngredients(0)
                .score(BigDecimal.ONE)
                .nearestExpireDate(stockProduct1.getExpireDate())
                .build();

        IngredientResponseDto ingDto = IngredientResponseDto.builder()
                .id(1)
                .recipeId(10)
                .productId(10)
                .productName("Peito de Frango")
                .build();

        when(stockRepository.findById(1)).thenReturn(Optional.of(stock));
        when(userGroupRepository.existsByUserIdAndGroupId(user.getId(), group.getId())).thenReturn(true);
        when(stockProductRepository.findByStockId(1)).thenReturn(List.of(stockProduct1, stockProduct2));
        when(frigusAiClient.requestRecipeFromAi(any(FrigusAiPromptPayload.class))).thenReturn(aiResponse);
        when(recipeSuggestionRepository.findFirstByStockIdOrderByIdDesc(1)).thenReturn(Optional.of(aiSuggestion));
        when(ingredientService.findByRecipeId(10)).thenReturn(List.of(ingDto));

        AiRecipeChatResponseDto response = service.processUserMessage(user, requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("ai-session-123");
        assertThat(response.getChatMessage()).isEqualTo("Encontrei ótimas receitas no seu estoque!");
        assertThat(response.getRecipeId()).isEqualTo(10);
        assertThat(response.getRecipeName()).isEqualTo("Refeição Prática");
        assertThat(response.getIngredients()).hasSize(1);
        assertThat(response.getIngredients().get(0).getInStock()).isTrue();
        assertThat(response.getIngredients().get(0).getExpireDate()).isEqualTo(stockProduct1.getExpireDate());
        assertThat(response.getNearestExpireDate()).isEqualTo(stockProduct1.getExpireDate());
    }
}
