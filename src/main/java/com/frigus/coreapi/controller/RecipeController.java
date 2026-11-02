package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.recipe.*;
import com.frigus.coreapi.service.RecipeService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import java.util.List;
@RestController @RequestMapping("/recipes") @RequiredArgsConstructor @Validated
public class RecipeController {
 private final RecipeService service;
 @GetMapping public Page<RecipeResponseDto> list(@RequestParam(required=false) String name,Pageable pageable){return service.list(name,pageable);}
 @GetMapping("/{id}") public RecipeResponseDto get(@PathVariable Integer id){return service.get(id);}
 @GetMapping("/favorites") public Page<RecipeResponseDto> favorites(Pageable pageable){return service.favorites(pageable);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") public RecipeResponseDto create(@Valid @RequestBody RecipeRequestDto dto){return service.create(dto);}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public RecipeResponseDto update(@PathVariable Integer id,@Valid @RequestBody RecipeRequestDto dto){return service.update(id,dto);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Integer id){service.delete(id);}
 @PutMapping("/{id}/favorite") @ResponseStatus(HttpStatus.NO_CONTENT) public void favorite(@PathVariable Integer id){service.favorite(id);}
 @DeleteMapping("/{id}/favorite") @ResponseStatus(HttpStatus.NO_CONTENT) public void unfavorite(@PathVariable Integer id){service.unfavorite(id);}
 @GetMapping("/{id}/availability") public List<IngredientAvailabilityDto> availability(@PathVariable Integer id,@RequestParam(required=false) Integer stockId){return service.availability(id,stockId);}
 @GetMapping("/suggestions") public List<RecipeSuggestionDto> suggestions(@RequestParam(required=false) Integer stockId,@RequestParam(defaultValue="10") @Min(1) @Max(50) int limit){return service.suggestions(stockId,limit);}
}
