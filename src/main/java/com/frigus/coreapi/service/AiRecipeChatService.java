package com.frigus.coreapi.service;

import com.frigus.coreapi.client.FrigusAiClient;
import com.frigus.coreapi.dto.ai.AiRecipeChatRequestDto;
import com.frigus.coreapi.dto.ai.AiRecipeChatResponseDto;
import com.frigus.coreapi.dto.ai.FrigusAiPromptPayload;
import com.frigus.coreapi.dto.ai.FrigusAiRecipeResponse;
import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.exception.ServiceUnavailableException;
import com.frigus.coreapi.model.Recipe;
import com.frigus.coreapi.model.RecipeSuggestion;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.RecipeRepository;
import com.frigus.coreapi.repository.RecipeSuggestionRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import com.frigus.coreapi.repository.StockRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecipeChatService {

    private final StockRepository stockRepository;
    private final StockProductRepository stockProductRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientService ingredientService;
    private final RecipeSuggestionRepository recipeSuggestionRepository;
    private final FrigusAiClient frigusAiClient;
    private final UserGroupRepository userGroupRepository;

    @Transactional
    public AiRecipeChatResponseDto processUserMessage(User currentUser, AiRecipeChatRequestDto dto) {
        Stock stock = stockRepository.findById(dto.getStockId())
                .orElseThrow(() -> new NotFoundException("Estoque não encontrado", "Nenhum estoque foi encontrado com o ID informado"));

        validateUserAccess(currentUser, stock);

        List<StockProduct> stockProducts = stockProductRepository.findByStockId(stock.getId());
        log.info("AI stock context: stockId={}, lotCount={}, positiveQuantityLots={}", stock.getId(), stockProducts.size(),
                stockProducts.stream().filter(item -> item.getQuantity() != null && item.getQuantity() > 0).count());

        // Monta o payload para o serviço de IA com itens ordenados por proximidade da validade
        List<FrigusAiPromptPayload.StockItemPayload> itemsPayload = stockProducts.stream()
                .sorted(Comparator.comparing(StockProduct::getExpireDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(sp -> FrigusAiPromptPayload.StockItemPayload.builder()
                        .productId(sp.getProduct().getId())
                        .productName(sp.getProduct().getName())
                        .category(sp.getProduct().getCategory() != null ? sp.getProduct().getCategory().name() : null)
                        .quantity(sp.getQuantity())
                        .expireDate(sp.getExpireDate())
                        .status(sp.getProductStatus() != null ? sp.getProductStatus().name() : null)
                        .build())
                .toList();

        FrigusAiPromptPayload promptPayload = FrigusAiPromptPayload.builder()
                .userId(currentUser != null && currentUser.getId() != null ? currentUser.getId().toString() : null)
                .message(dto.getMessage())
                .sessionId(dto.getSessionId())
                .stockId(stock.getId())
                .foodPreferences(dto.getFoodPreferences())
                .availableItems(itemsPayload)
                .build();

        FrigusAiRecipeResponse aiResponse = frigusAiClient.requestRecipeFromAi(promptPayload);
        if (aiResponse.getRecipeIds() != null) {
            return saveReportedSuggestions(aiResponse, stock, stockProducts);
        }

        Recipe recipe;
        final List<IngredientResponseDto> savedIngredients = new ArrayList<>();
        RecipeSuggestion suggestion;
        int matched;
        int missing;
        BigDecimal score;
        LocalDate nearestExpire;

        Optional<RecipeSuggestion> latestAiSuggestion = Optional.empty();
        if (aiResponse.getRecipeName() == null || aiResponse.getRecipeName().isBlank()) {
            List<RecipeSuggestion> recent = recipeSuggestionRepository.findTop5ByStockIdOrderByIdDesc(stock.getId());
            if (recent != null && !recent.isEmpty()) {
                String chatMsg = aiResponse.getChatMessage() != null ? aiResponse.getChatMessage().toLowerCase() : "";
                latestAiSuggestion = recent.stream()
                        .filter(s -> s.getRecipe() != null && s.getRecipe().getName() != null &&
                                !s.getRecipe().getName().isBlank() &&
                                chatMsg.contains(s.getRecipe().getName().toLowerCase()))
                        .findFirst();
            }
            if (latestAiSuggestion.isEmpty()) {
                return AiRecipeChatResponseDto.builder()
                        .sessionId(aiResponse.getSessionId())
                        .chatMessage(aiResponse.getChatMessage())
                        .build();
            }
        }

        if (latestAiSuggestion.isPresent()) {
            suggestion = latestAiSuggestion.get();
            recipe = suggestion.getRecipe();
            savedIngredients.addAll(ingredientService.findByRecipeId(recipe.getId()));
            matched = suggestion.getMatchedIngredients() != null ? suggestion.getMatchedIngredients() : 0;
            missing = suggestion.getMissingIngredients() != null ? suggestion.getMissingIngredients() : 0;
            score = suggestion.getScore() != null ? suggestion.getScore() : BigDecimal.ZERO;
            nearestExpire = suggestion.getNearestExpireDate();
        } else {
            recipe = Recipe.builder()
                    .name(aiResponse.getRecipeName() != null ? aiResponse.getRecipeName() : "Receita Sugerida Inteligente")
                    .description(aiResponse.getDescription())
                    .instructions(aiResponse.getInstructions())
                    .domesticOnly(true)
                    .active(true)
                    .createdAt(Instant.now())
                    .build();
            recipe = recipeRepository.save(recipe);

            if (aiResponse.getIngredients() != null) {
                for (FrigusAiRecipeResponse.AiRecipeIngredientPayload item : aiResponse.getIngredients()) {
                    if (item.getProductId() != null) {
                        try {
                            IngredientRequestDto ingDto = IngredientRequestDto.builder()
                                    .recipeId(recipe.getId())
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE)
                                    .unit(item.getUnit())
                                    .required(item.getRequired() != null ? item.getRequired() : true)
                                    .build();
                            savedIngredients.add(ingredientService.create(ingDto));
                        } catch (Exception e) {
                            log.warn("Erro ao associar ingrediente do produto ID {}: {}", item.getProductId(), e.getMessage());
                        }
                    }
                }
            }

            Set<Integer> availableProductIds = stockProducts.stream()
                    .map(sp -> sp.getProduct().getId())
                    .collect(Collectors.toSet());

            matched = (int) savedIngredients.stream()
                    .filter(ing -> availableProductIds.contains(ing.getProductId()))
                    .count();
            int total = savedIngredients.size();
            missing = Math.max((total - matched), 0);

            score = total > 0
                    ? BigDecimal.valueOf(matched).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Set<Integer> ingredientProductIds = savedIngredients.stream()
                    .map(IngredientResponseDto::getProductId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            nearestExpire = stockProducts.stream()
                    .filter(sp -> sp.getProduct() != null && ingredientProductIds.contains(sp.getProduct().getId()))
                    .map(StockProduct::getExpireDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            suggestion = RecipeSuggestion.builder()
                    .recipe(recipe)
                    .stock(stock)
                    .matchedIngredients(matched)
                    .missingIngredients(missing)
                    .nearestExpireDate(nearestExpire)
                    .score(score)
                    .createdAt(Instant.now())
                    .build();
            suggestion = recipeSuggestionRepository.save(suggestion);
        }

        // Enriquece cada ingrediente com status de estoque e data de validade
        Map<Integer, LocalDate> stockExpireMap = stockProducts.stream()
                .filter(sp -> sp.getProduct() != null && sp.getProduct().getId() != null && sp.getExpireDate() != null)
                .collect(Collectors.toMap(
                        sp -> sp.getProduct().getId(),
                        StockProduct::getExpireDate,
                        (d1, d2) -> d1.isBefore(d2) ? d1 : d2
                ));

        Set<Integer> availableProductIds = stockProducts.stream()
                .filter(sp -> sp.getProduct() != null && sp.getProduct().getId() != null)
                .map(sp -> sp.getProduct().getId())
                .collect(Collectors.toSet());

        for (IngredientResponseDto ing : savedIngredients) {
            boolean inStock = ing.getProductId() != null && availableProductIds.contains(ing.getProductId());
            ing.setInStock(inStock);
            if (inStock) {
                ing.setExpireDate(stockExpireMap.get(ing.getProductId()));
            }
        }

        return AiRecipeChatResponseDto.builder()
                .sessionId(aiResponse.getSessionId())
                .chatMessage(aiResponse.getChatMessage())
                .recipeId(recipe.getId())
                .recipeName(recipe.getName())
                .recipeDescription(recipe.getDescription())
                .recipeInstructions(recipe.getInstructions())
                .ingredients(savedIngredients)
                .suggestionId(suggestion.getId())
                .matchedIngredients(matched)
                .missingIngredients(missing)
                .score(score)
                .nearestExpireDate(nearestExpire)
                .build();
    }

    private AiRecipeChatResponseDto saveReportedSuggestions(FrigusAiRecipeResponse reply, Stock stock,
                                                            List<StockProduct> products) {
        Map<Integer, LocalDate> available = products.stream()
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0 && p.getExpireDate() != null &&
                        !p.getExpireDate().isBefore(LocalDate.now()))
                .collect(Collectors.toMap(p -> p.getProduct().getId(), StockProduct::getExpireDate,
                        (a, b) -> a.isBefore(b) ? a : b));
        List<Integer> ids = reply.getRecipeIds().stream().distinct().toList();
        AiRecipeChatResponseDto result = AiRecipeChatResponseDto.builder()
                .sessionId(reply.getSessionId()).chatMessage(reply.getChatMessage()).recipeIds(ids).build();
        for (Integer id : ids) {
            Recipe recipe = recipeRepository.findById(id)
                    .filter(r -> !Boolean.FALSE.equals(r.getActive()))
                    .orElseThrow(() -> new ServiceUnavailableException("Sugestão não confirmada", "Não foi possível confirmar a receita sugerida. Consulte as receitas salvas antes de tentar novamente."));
            List<IngredientResponseDto> ingredients = ingredientService.findByRecipeId(id);
            int matched = 0;
            LocalDate nearest = null;
            for (IngredientResponseDto ingredient : ingredients) {
                LocalDate expiry = available.get(ingredient.getProductId());
                ingredient.setInStock(expiry != null);
                ingredient.setExpireDate(expiry);
                if (expiry != null) {
                    matched++;
                    if (nearest == null || expiry.isBefore(nearest)) nearest = expiry;
                }
            }
            int missing = ingredients.size() - matched;
            BigDecimal score = ingredients.isEmpty() ? BigDecimal.ZERO :
                    BigDecimal.valueOf(matched).divide(BigDecimal.valueOf(ingredients.size()), 3, RoundingMode.HALF_UP);
            RecipeSuggestion suggestion = recipeSuggestionRepository
                    .findFirstByStockIdAndRecipeIdOrderByIdDesc(stock.getId(), id)
                    .orElseGet(() -> RecipeSuggestion.builder().recipe(recipe).stock(stock).createdAt(Instant.now()).build());
            suggestion.setMatchedIngredients(matched);
            suggestion.setMissingIngredients(missing);
            suggestion.setScore(score);
            suggestion.setNearestExpireDate(nearest);
            suggestion = recipeSuggestionRepository.save(suggestion);
            if (result.getRecipeId() == null) {
                result.setRecipeId(id);
                result.setRecipeName(recipe.getName());
                result.setRecipeDescription(recipe.getDescription());
                result.setRecipeInstructions(recipe.getInstructions());
                result.setIngredients(ingredients);
                result.setSuggestionId(suggestion.getId());
                result.setMatchedIngredients(matched);
                result.setMissingIngredients(missing);
                result.setScore(score);
                result.setNearestExpireDate(nearest);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<AiRecipeChatResponseDto> getSuggestionsByStock(User currentUser, Integer stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new NotFoundException("Estoque não encontrado", "Nenhum estoque foi encontrado com o ID informado"));

        validateUserAccess(currentUser, stock);

        List<StockProduct> stockProducts = stockProductRepository.findByStockId(stock.getId());
        Map<Integer, LocalDate> stockExpireMap = stockProducts.stream()
                .filter(sp -> sp.getProduct() != null && sp.getProduct().getId() != null && sp.getExpireDate() != null &&
                        sp.getQuantity() != null && sp.getQuantity() > 0 && !sp.getExpireDate().isBefore(LocalDate.now()))
                .collect(Collectors.toMap(
                        sp -> sp.getProduct().getId(),
                        StockProduct::getExpireDate,
                        (d1, d2) -> d1.isBefore(d2) ? d1 : d2
                ));
        Set<Integer> availableProductIds = stockExpireMap.keySet();

        return recipeSuggestionRepository.findByStockIdOrderByScoreDesc(stockId).stream()
                .map(sugg -> {
                    List<IngredientResponseDto> ingredients = new ArrayList<>(ingredientService.findByRecipeId(sugg.getRecipe().getId()));
                    for (IngredientResponseDto ing : ingredients) {
                        boolean inStock = ing.getProductId() != null && availableProductIds.contains(ing.getProductId());
                        ing.setInStock(inStock);
                        ing.setExpireDate(stockExpireMap.get(ing.getProductId()));
                    }

                    return AiRecipeChatResponseDto.builder()
                            .recipeId(sugg.getRecipe().getId())
                            .recipeName(sugg.getRecipe().getName())
                            .recipeDescription(sugg.getRecipe().getDescription())
                            .recipeInstructions(sugg.getRecipe().getInstructions())
                            .ingredients(ingredients)
                            .suggestionId(sugg.getId())
                            .matchedIngredients(sugg.getMatchedIngredients())
                            .missingIngredients(sugg.getMissingIngredients())
                            .score(sugg.getScore())
                            .nearestExpireDate(sugg.getNearestExpireDate())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AiRecipeChatResponseDto> getSuggestionsByStock(Integer stockId) {
        return getSuggestionsByStock(null, stockId);
    }

    private void validateUserAccess(User currentUser, Stock stock) {
        if (currentUser != null && currentUser.getId() != null && stock.getGroup() != null && stock.getGroup().getId() != null) {
            boolean hasAccess = userGroupRepository.existsByUserIdAndGroupId(currentUser.getId(), stock.getGroup().getId());
            if (!hasAccess) {
                throw new ForbiddenException("Acesso negado", "Você não tem permissão para acessar o estoque deste grupo");
            }
        }
    }
}
