package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.recipe.*;
import com.frigus.coreapi.exception.*;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import com.frigus.coreapi.mapper.IngredientMapper;
import com.frigus.coreapi.utils.IngredientUnits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
import java.math.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class RecipeService {
 @org.springframework.beans.factory.annotation.Value("${app.time-zone:America/Sao_Paulo}") private String timeZone="America/Sao_Paulo";
 private final RecipeRepository recipes;
 private final RecipeIngredientRepository ingredients;
 private final RecipeFavoriteRepository favorites;
 private final StockProductRepository stocks;
 private final UserRepository users;
 private final IngredientMapper ingredientMapper;
 private final GroupContextService context;
 private final GroupAccessService access;
 private final PlanLimitsResolverService limits;
 public Page<RecipeResponseDto> list(String name,Pageable pageable){
  var page=name==null ? recipes.findByActiveTrue(pageable) : recipes.findByActiveTrueAndNameContainingIgnoreCase(name,pageable);
  var content=toDtos(page.getContent());
  return new PageImpl<>(content,page.getPageable(),page.getTotalElements());
 }
 public RecipeResponseDto get(Integer id){return toDto(requireRecipe(id));}
 public Page<RecipeResponseDto> favorites(Pageable pageable){return favorites.findByUserIdAndRecipeActiveTrue(access.requireCurrentUser().getId(),pageable).map(f -> toDto(f.getRecipe()));}
 @Transactional public RecipeResponseDto create(RecipeRequestDto dto){
  var recipe=Recipe.builder().name(dto.getName().trim()).description(dto.getDescription()).instructions(dto.getInstructions()).domesticOnly(dto.getDomesticOnly() != null ? dto.getDomesticOnly() : true).active(dto.getActive() != null ? dto.getActive() : true).createdAt(Instant.now()).build();
  return toDto(recipes.save(recipe));
 }
 @Transactional public RecipeResponseDto update(Integer id,RecipeRequestDto dto){
  var recipe=recipes.findById(id).orElseThrow(NotFoundException::new);
  recipe.setName(dto.getName().trim());recipe.setDescription(dto.getDescription());recipe.setInstructions(dto.getInstructions());recipe.setDomesticOnly(dto.getDomesticOnly());recipe.setActive(dto.getActive());return toDto(recipes.save(recipe));
 }
 @Transactional public void delete(Integer id){var recipe=recipes.findById(id).orElseThrow(NotFoundException::new);recipe.setActive(false);recipes.save(recipe);}
 @Transactional public void favorite(Integer id){
  if(!limits.resolveLimitsForCurrentUser().isAllowSavedRecipes()) throw new ForbiddenException("Plano não permite favoritos","Escolha um plano com receitas salvas");
  var user=users.findByIdForUpdate(access.requireCurrentUser().getId()).orElseThrow(NotFoundException::new);
  var recipe=requireRecipe(id);
  if(!favorites.existsByUserIdAndRecipeId(user.getId(),id)) favorites.save(RecipeFavorite.builder().user(user).recipe(recipe).build());
 }
 @Transactional public void unfavorite(Integer id){
  var user=users.findByIdForUpdate(access.requireCurrentUser().getId()).orElseThrow(NotFoundException::new);
  favorites.findByUserIdAndRecipeId(user.getId(),id).ifPresent(favorites::delete);
 }
 public List<IngredientAvailabilityDto> availability(Integer id,Integer stockId){return calculate(ingredients.findByRecipeId(requireRecipe(id).getId()),inventory(stockId));}
 public List<RecipeSuggestionDto> suggestions(Integer stockId,int limit){
  var inventory=inventory(stockId);
  var allIngredients=ingredients.findActiveRecipeIngredients().stream().collect(java.util.stream.Collectors.groupingBy(i -> i.getRecipe().getId()));
  List<ScoredRecipe> result=new ArrayList<>();
  for(var recipe:recipes.findByActiveTrue()){
   var availability=calculate(allIngredients.getOrDefault(recipe.getId(),List.of()),inventory);
   int matched=(int)availability.stream().filter(i -> i.required() && i.available()).count();
   int missing=(int)availability.stream().filter(i -> i.required() && !i.available()).count();
   BigDecimal score=matched+missing==0 ? BigDecimal.ZERO : BigDecimal.valueOf(matched).divide(BigDecimal.valueOf(matched+missing),4,RoundingMode.HALF_UP);
   result.add(new ScoredRecipe(recipe,matched,missing,score,availability));
  }
  var selected=result.stream().sorted(Comparator.comparing(ScoredRecipe::score).reversed().thenComparing(s -> s.recipe().getId())).limit(limit).toList();
  var recipeDtos=toDtos(selected.stream().map(ScoredRecipe::recipe).toList());
  List<RecipeSuggestionDto> suggestions=new ArrayList<>(selected.size());
  for(int i=0;i<selected.size();i++){
   var scored=selected.get(i);
   suggestions.add(new RecipeSuggestionDto(recipeDtos.get(i),scored.matched(),scored.missing(),scored.score(),scored.availability()));
  }
  return suggestions;
 }
 private Map<Integer,Integer> inventory(Integer stockId){
  var all=stockId==null ? stocks.findActiveByGroup(context.resolveGroup(null).getId()) : stocks.findActiveByStock(context.requireStock(stockId,false).getId());
  Map<Integer,Integer> available=new HashMap<>();
  for(var item:all) if(item.getQuantity()>0 && !item.getExpireDate().isBefore(LocalDate.now(ZoneId.of(timeZone)))) available.merge(item.getProduct().getId(),item.getQuantity(),Math::addExact);
  return available;
 }
 private List<IngredientAvailabilityDto> calculate(List<RecipeIngredient> recipeIngredients,Map<Integer,Integer> stock){
  return recipeIngredients.stream().map(i -> {
   int quantity=stock.getOrDefault(i.getProduct().getId(),0);
   var converted=IngredientUnits.convert(quantity,i.getProduct().getUnitOfMeasure(),i.getUnit());
   boolean available=quantity>0 && (i.getQuantity()==null || converted!=null && converted.compareTo(i.getQuantity())>=0);
   return new IngredientAvailabilityDto(i.getProduct().getId(),i.getProduct().getName(),i.getQuantity(),quantity,i.getUnit(),Boolean.TRUE.equals(i.getRequired()),available);
  }).toList();
 }
 private Recipe requireRecipe(Integer id){return recipes.findByIdAndActiveTrue(id).orElseThrow(NotFoundException::new);}
 private RecipeResponseDto toDto(Recipe recipe){return toDtos(List.of(recipe)).get(0);}
 private List<RecipeResponseDto> toDtos(List<Recipe> recipesToMap){
  if(recipesToMap.isEmpty()) return List.of();
  var recipeIds=recipesToMap.stream().map(Recipe::getId).toList();
  var ingredientsByRecipe=ingredients.findByRecipeIdsWithProduct(recipeIds).stream()
   .collect(java.util.stream.Collectors.groupingBy(i -> i.getRecipe().getId(),java.util.stream.Collectors.mapping(ingredientMapper::toDto,java.util.stream.Collectors.toList())));
  var favoriteRecipeIds=favorites.findRecipeIdsByUserIdAndRecipeIds(access.requireCurrentUser().getId(),recipeIds);
  return recipesToMap.stream().map(recipe -> RecipeResponseDto.builder().id(recipe.getId()).name(recipe.getName()).description(recipe.getDescription()).instructions(recipe.getInstructions()).domesticOnly(recipe.getDomesticOnly()).active(recipe.getActive()).favorite(favoriteRecipeIds.contains(recipe.getId())).createdAt(recipe.getCreatedAt()).ingredients(ingredientsByRecipe.getOrDefault(recipe.getId(),List.of())).build()).toList();
 }
 private record ScoredRecipe(Recipe recipe,int matched,int missing,BigDecimal score,List<IngredientAvailabilityDto> availability) { }
}
