package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.dto.ingredient.IngredientUpdateRequestDto;
import com.frigus.coreapi.model.RecipeIngredient;
import com.frigus.coreapi.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredients")
@Tag(name = "Ingredients", description = "Endpoints para gerenciamento de ingredientes das receitas")
public class IngredientController extends BaseController<RecipeIngredient, Integer, IngredientRequestDto, IngredientResponseDto, IngredientService> {

    public IngredientController(IngredientService service) {
        super(service);
    }

    @Override
    @GetMapping
    public Page<IngredientResponseDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @Override
    @GetMapping("/{id}")
    public IngredientResponseDto findById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponseDto create(@Valid @RequestBody IngredientRequestDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public IngredientResponseDto update(
            @PathVariable Integer id,
            @Valid @RequestBody IngredientUpdateRequestDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/recipe/{recipeId}")
    public List<IngredientResponseDto> findByRecipeId(@PathVariable Integer recipeId) {
        return service.findByRecipeId(recipeId);
    }

    @GetMapping("/product/{productId}")
    public List<IngredientResponseDto> findByProductId(@PathVariable Integer productId) {
        return service.findByProductId(productId);
    }
}
