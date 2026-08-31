package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.model.RecipeIngredient;
import com.frigus.coreapi.service.IngredientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController extends BaseController<RecipeIngredient, Integer, IngredientRequestDto, IngredientResponseDto, IngredientService> {
    public IngredientController(IngredientService service) {
        super(service);
    }

    @GetMapping("/recipe/{recipeId}")
    public List<IngredientResponseDto> findByRecipeId(@PathVariable Integer recipeId) {
        return service.findByRecipeId(recipeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponseDto create(@Valid @RequestBody IngredientRequestDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public IngredientResponseDto update(
            @PathVariable Integer id,
            @Valid @RequestBody IngredientRequestDto dto) {
        return service.update(id, dto);
    }
}
