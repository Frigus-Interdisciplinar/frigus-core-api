package com.frigus.coreapi.dto.recipe;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record RecipeSuggestionDto(RecipeResponseDto recipe, int matchedIngredients, int missingIngredients, BigDecimal score, List<IngredientAvailabilityDto> availability) { }
