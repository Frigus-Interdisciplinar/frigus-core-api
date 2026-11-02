package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.preferences.AppSettingsDto;
import com.frigus.coreapi.model.UserAppSettings;
import com.frigus.coreapi.repository.UserAppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AppSettingsService {
    private final UserAppSettingsRepository repository;
    private final GroupAccessService access;

    @Transactional(readOnly = true)
    public AppSettingsDto get() {
        return toDto(settingsForCurrentUser());
    }

    @Transactional
    public AppSettingsDto update(AppSettingsDto request) {
        var current = settingsForCurrentUser();
        var food = request.foodPreferences();
        current.setDietaryPreferences(food.preferences().stream().distinct().toList());
        current.setDietaryRestrictions(food.restrictions().stream().distinct().toList());
        current.setQuickRecipesEnabled(food.quickRecipesEnabled());
        current.setBudgetRecipesEnabled(food.budgetRecipesEnabled());
        current.setWeightUnit(request.units().weight());
        current.setVolumeUnit(request.units().volume());
        current.setTemperatureUnit(request.units().temperature());
        current.setUsageDataCollectionAllowed(request.privacy().usageDataCollectionAllowed());
        current.setPersonalizedSuggestionsAllowed(request.privacy().personalizedSuggestionsAllowed());
        current.setUpdatedAt(Instant.now());
        return toDto(repository.save(current));
    }

    private UserAppSettings settingsForCurrentUser() {
        var userId = access.requireCurrentUser().getId();
        return repository.findById(userId).orElseGet(() -> UserAppSettings.builder().userId(userId).build());
    }

    private AppSettingsDto toDto(UserAppSettings settings) {
        return new AppSettingsDto(
                new AppSettingsDto.FoodPreferences(
                        settings.getDietaryPreferences(), settings.getDietaryRestrictions(),
                        settings.isQuickRecipesEnabled(), settings.isBudgetRecipesEnabled()),
                new AppSettingsDto.MeasurementUnits(
                        settings.getWeightUnit(), settings.getVolumeUnit(), settings.getTemperatureUnit()),
                new AppSettingsDto.PrivacySettings(
                        settings.isUsageDataCollectionAllowed(), settings.isPersonalizedSuggestionsAllowed()));
    }
}
