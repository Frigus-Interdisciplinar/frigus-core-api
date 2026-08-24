package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.ingredient.IngredientRequestDto;
import com.frigus.coreapi.dto.ingredient.IngredientResponseDto;
import com.frigus.coreapi.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {
    private final IngredientService service;

    @GetMapping
    public ResponseEntity<Page<IngredientResponseDto>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<List<IngredientResponseDto>> findByRecipeId(@PathVariable Integer recipeId) {
        return ResponseEntity.ok(service.findByRecipeId(recipeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<IngredientResponseDto> create(@Valid @RequestBody IngredientRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientResponseDto> update(
            @PathVariable Integer id,
            @Valid @RequestBody IngredientRequestDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
