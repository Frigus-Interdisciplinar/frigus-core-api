package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.recipe.RecipeRequestDto;
import com.frigus.coreapi.dto.recipe.RecipeResponseDto;
import com.frigus.coreapi.model.Recipe;
import com.frigus.coreapi.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
