package com.frigus.coreapi.dto.preferences;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AppSettingsDto(
        @NotNull @Valid FoodPreferences foodPreferences,
        @NotNull @Valid MeasurementUnits units,
        @NotNull @Valid PrivacySettings privacy
) {
    public record FoodPreferences(
            @NotNull @Size(max = 4) List<@NotNull @Pattern(regexp = "VEGETARIAN|VEGAN|AVOID_RED_MEAT|AVOID_SEAFOOD") String> preferences,
            @NotNull @Size(max = 3) List<@NotNull @Pattern(regexp = "LACTOSE_FREE|GLUTEN_FREE|SUGAR_FREE") String> restrictions,
            boolean quickRecipesEnabled,
            boolean budgetRecipesEnabled
    ) {}

    public record MeasurementUnits(
            @NotNull @Pattern(regexp = "KG|G|LB|OZ") String weight,
            @NotNull @Pattern(regexp = "L|ML|CUP|SPOON") String volume,
            @NotNull @Pattern(regexp = "CELSIUS|FAHRENHEIT") String temperature
    ) {}

    public record PrivacySettings(
            boolean usageDataCollectionAllowed,
            boolean personalizedSuggestionsAllowed
    ) {}
}
