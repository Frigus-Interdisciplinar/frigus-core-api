package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.shopping.ShoppingListProductRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListProductResponseDto;
import com.frigus.coreapi.service.ShoppingListProductService;
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
import java.util.UUID;

@RestController
@RequestMapping("/shopping-list-products")
@RequiredArgsConstructor
public class ShoppingListProductController {
    private final ShoppingListProductService service;

    @GetMapping
    public ResponseEntity<Page<ShoppingListProductResponseDto>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<List<ShoppingListProductResponseDto>> findByListId(@PathVariable UUID listId) {
        return ResponseEntity.ok(service.findByListId(listId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListProductResponseDto> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ShoppingListProductResponseDto> create(
            @Valid @RequestBody ShoppingListProductRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShoppingListProductResponseDto> update(
            @PathVariable Integer id,
            @Valid @RequestBody ShoppingListProductRequestDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
