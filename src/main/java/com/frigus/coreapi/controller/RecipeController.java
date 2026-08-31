package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.recipe.RecipeRequestDto;
import com.frigus.coreapi.dto.recipe.RecipeResponseDto;
import com.frigus.coreapi.model.Recipe;
import com.frigus.coreapi.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recipes")
public class RecipeController extends BaseController<Recipe, Integer, RecipeRequestDto, RecipeResponseDto, RecipeService> {
    public RecipeController(RecipeService service) {
        super(service);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponseDto create(@Valid @RequestBody RecipeRequestDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public RecipeResponseDto update(
            @PathVariable Integer id,
            @Valid @RequestBody RecipeRequestDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteRecipe(@PathVariable Integer id) {
        service.deleteRecipe(id);
    }
}
