package com.frigus.coreapi.repository;
import com.frigus.coreapi.model.*;
import java.util.*;
import java.time.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface RecipeFavoriteRepository extends BaseRepository<RecipeFavorite,UUID> {
 Optional<RecipeFavorite> findByUserIdAndRecipeId(UUID userId,Integer recipeId);
 boolean existsByUserIdAndRecipeId(UUID userId,Integer recipeId);
 Page<RecipeFavorite> findByUserIdAndRecipeActiveTrue(UUID userId,Pageable pageable);

}
