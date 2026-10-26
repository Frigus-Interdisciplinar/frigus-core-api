package com.frigus.coreapi.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name="recipe_favorites")
public class RecipeFavorite {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="recipe_id") private Recipe recipe;
 @Builder.Default @Column(nullable=false) private Instant createdAt=Instant.now();
}
