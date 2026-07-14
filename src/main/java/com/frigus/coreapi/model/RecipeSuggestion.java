package com.frigus.coreapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "recipe_suggestions")
public class RecipeSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "matched_ingredients", nullable = false)
    private Integer matchedIngredients;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "missing_ingredients", nullable = false)
    private Integer missingIngredients;

    @Column(name = "nearest_expire_date")
    private LocalDate nearestExpireDate;

    @Column(name = "score")
    private BigDecimal score;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}