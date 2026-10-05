package com.frigus.coreapi.controller;

<<<<<<< HEAD
import com.frigus.coreapi.dto.shoppinglist.ShoppingListCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductResponseDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductUpdateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListResponseDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListUpdateRequestDto;
import com.frigus.coreapi.enums.ListStatus;
import com.frigus.coreapi.enums.ProductListStatus;
import com.frigus.coreapi.service.ShoppingListProductService;
import com.frigus.coreapi.service.ShoppingListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
=======
import com.frigus.coreapi.dto.shopping.ShoppingListRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListResponseDto;
import com.frigus.coreapi.service.ShoppingListService;
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
<<<<<<< HEAD
import org.springframework.web.bind.annotation.*;

import java.util.List;
=======
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
import java.util.UUID;

@RestController
@RequestMapping("/shopping-lists")
@RequiredArgsConstructor
<<<<<<< HEAD
@Tag(name = "Shopping Lists", description = "Endpoints for managing shopping lists and their items")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final ShoppingListProductService shoppingListProductService;

    // --- LIST ENDPOINTS ---

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new shopping list")
    public ShoppingListResponseDto create(@Valid @RequestBody ShoppingListCreateRequestDto dto) {
        return shoppingListService.create(dto);
    }

    @GetMapping
    @Operation(summary = "Get a paginated list of shopping lists")
    public Page<ShoppingListResponseDto> findAll(
            @RequestParam(required = false) Integer stockId,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) ListStatus status,
            Pageable pageable) {
        return shoppingListService.findAll(stockId, groupId, status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific shopping list with its items and counters")
    public ShoppingListResponseDto findById(@PathVariable UUID id) {
        return shoppingListService.findByIdWithItems(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update shopping list date or status")
    public ShoppingListResponseDto update(@PathVariable UUID id, @Valid @RequestBody ShoppingListUpdateRequestDto dto) {
        return shoppingListService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a shopping list")
    public void delete(@PathVariable UUID id) {
        shoppingListService.delete(id);
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Manually close a shopping list")
    public ShoppingListResponseDto closeList(@PathVariable UUID id) {
        return shoppingListService.closeList(id);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Manually cancel a shopping list")
    public ShoppingListResponseDto cancelList(@PathVariable UUID id) {
        return shoppingListService.cancelList(id);
    }

    @PatchMapping("/{id}/reopen")
    @Operation(summary = "Manually reopen a shopping list")
    public ShoppingListResponseDto reopenList(@PathVariable UUID id) {
        return shoppingListService.reopenList(id);
    }

    @PostMapping("/from-low-stock")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate a new shopping list automatically from low stock products")
    public ShoppingListResponseDto generateFromLowStock(@RequestParam Integer stockId) {
        return shoppingListService.generateFromLowStock(stockId);
    }

    // --- ITEM ENDPOINTS ---

    @PostMapping("/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an item to a shopping list")
    public ShoppingListProductResponseDto addItem(@PathVariable UUID listId, @Valid @RequestBody ShoppingListProductCreateRequestDto dto) {
        return shoppingListProductService.addItem(listId, dto);
    }
    
    @PostMapping("/{listId}/items/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add multiple items to a shopping list")
    public void addItemBatch(@PathVariable UUID listId, @Valid @RequestBody List<ShoppingListProductCreateRequestDto> items) {
        shoppingListProductService.addItemBatch(listId, items);
    }

    @GetMapping("/{listId}/items")
    @Operation(summary = "Get all items of a shopping list (paginated)")
    public Page<ShoppingListProductResponseDto> findItemsByListId(@PathVariable UUID listId, Pageable pageable) {
        return shoppingListProductService.findByListId(listId, pageable);
    }

    @PutMapping("/{listId}/items/{itemId}")
    @Operation(summary = "Update an item's quantity or status")
    public ShoppingListProductResponseDto updateItem(@PathVariable UUID listId, @PathVariable Integer itemId, @Valid @RequestBody ShoppingListProductUpdateRequestDto dto) {
        return shoppingListProductService.updateItem(listId, itemId, dto);
    }

    @PatchMapping("/{listId}/items/{itemId}/status")
    @Operation(summary = "Update an item's status")
    public ShoppingListProductResponseDto updateItemStatus(@PathVariable UUID listId, @PathVariable Integer itemId, @RequestParam ProductListStatus status) {
        return shoppingListProductService.updateStatus(listId, itemId, status);
    }

    @PatchMapping("/{listId}/items/{itemId}/purchased")
    @Operation(summary = "Shortcut to mark an item as purchased")
    public ShoppingListProductResponseDto markItemAsPurchased(@PathVariable UUID listId, @PathVariable Integer itemId) {
        return shoppingListProductService.updateStatus(listId, itemId, ProductListStatus.PURCHASED);
    }

    @DeleteMapping("/{listId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove an item from a shopping list")
    public void deleteItem(@PathVariable UUID listId, @PathVariable Integer itemId) {
        shoppingListProductService.deleteItem(listId, itemId);
=======
public class ShoppingListController {
    private final ShoppingListService service;

    @GetMapping
    public ResponseEntity<Page<ShoppingListResponseDto>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListResponseDto> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ShoppingListResponseDto> create(@Valid @RequestBody ShoppingListRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShoppingListResponseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody ShoppingListRequestDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
    }
}
