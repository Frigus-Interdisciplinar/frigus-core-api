package com.frigus.coreapi.service;

import com.frigus.coreapi.mapper.IngredientMapper;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Recipe;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {
    @Mock private RecipeRepository recipes;
    @Mock private RecipeIngredientRepository ingredients;
    @Mock private RecipeFavoriteRepository favorites;
    @Mock private StockProductRepository stocks;
    @Mock private UserRepository users;
    @Mock private IngredientMapper ingredientMapper;
    @Mock private GroupContextService context;
    @Mock private GroupAccessService access;
    @Mock private PlanLimitsResolverService limits;
    @InjectMocks private RecipeService service;

    @Test
    void suggestionsMapsOnlyTheRequestedResultsUsingBatchedRecipeDetails() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Group group = Group.builder().id(groupId).build();
        User user = User.builder().id(userId).build();
        List<Recipe> activeRecipes = IntStream.rangeClosed(1, 25)
                .mapToObj(id -> Recipe.builder().id(id).name("Recipe " + id).active(true).build())
                .toList();

        when(context.resolveGroup(null)).thenReturn(group);
        when(access.requireCurrentUser()).thenReturn(user);
        when(stocks.findActiveByGroup(groupId)).thenReturn(List.of());
        when(ingredients.findActiveRecipeIngredients()).thenReturn(List.of());
        when(recipes.findByActiveTrue()).thenReturn(activeRecipes);
        when(ingredients.findByRecipeIdsWithProduct(anyCollection())).thenReturn(List.of());
        when(favorites.findRecipeIdsByUserIdAndRecipeIds(eq(userId), anyCollection())).thenReturn(Set.of());

        var suggestions = service.suggestions(null, 5);

        assertThat(suggestions).hasSize(5);
        assertThat(suggestions).extracting(suggestion -> suggestion.recipe().getId())
                .containsExactly(1, 2, 3, 4, 5);
        verify(ingredients).findActiveRecipeIngredients();
        verify(ingredients).findByRecipeIdsWithProduct(anyCollection());
        verify(favorites).findRecipeIdsByUserIdAndRecipeIds(eq(userId), anyCollection());
        verify(ingredients, never()).findByRecipeId(anyInt());
        verify(favorites, never()).existsByUserIdAndRecipeId(any(), anyInt());
    }
}
