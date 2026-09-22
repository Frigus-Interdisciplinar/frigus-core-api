package com.frigus.coreapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {
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
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "unit", length = Integer.MAX_VALUE)
    private String unit;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "required", nullable = false)
    @Builder.Default
    private Boolean required = true;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private java.time.Instant createdAt = java.time.Instant.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private java.time.Instant updatedAt = java.time.Instant.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = java.time.Instant.now();
        }
        if (required == null) {
            required = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = java.time.Instant.now();
    }
}