package com.frigus.coreapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_app_settings")
public class UserAppSettings {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dietary_preferences", nullable = false, columnDefinition = "jsonb")
    private List<String> dietaryPreferences = new ArrayList<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dietary_restrictions", nullable = false, columnDefinition = "jsonb")
    private List<String> dietaryRestrictions = new ArrayList<>();

    @Builder.Default
    @Column(name = "quick_recipes_enabled", nullable = false)
    private boolean quickRecipesEnabled = true;

    @Builder.Default
    @Column(name = "budget_recipes_enabled", nullable = false)
    private boolean budgetRecipesEnabled = true;

    @Builder.Default
    @Column(name = "weight_unit", nullable = false, length = 8)
    private String weightUnit = "KG";

    @Builder.Default
    @Column(name = "volume_unit", nullable = false, length = 8)
    private String volumeUnit = "L";

    @Builder.Default
    @Column(name = "temperature_unit", nullable = false, length = 12)
    private String temperatureUnit = "CELSIUS";

    @Builder.Default
    @Column(name = "usage_data_collection_allowed", nullable = false)
    private boolean usageDataCollectionAllowed = true;

    @Builder.Default
    @Column(name = "personalized_suggestions_allowed", nullable = false)
    private boolean personalizedSuggestionsAllowed = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
