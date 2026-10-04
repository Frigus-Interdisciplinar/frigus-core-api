package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.shopping.*;
import com.frigus.coreapi.enums.ListStatus;
import com.frigus.coreapi.service.ShoppingListService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/shopping-lists") @RequiredArgsConstructor
public class ShoppingListController {
 private final ShoppingListService service;
 @GetMapping public Page<ShoppingListResponseDto> list(@RequestParam Integer stockId,@RequestParam(required=false) ListStatus status,Pageable pageable){return service.list(stockId,status,pageable);}
 @GetMapping("/{id}") public ShoppingListResponseDto get(@PathVariable UUID id){return service.get(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ShoppingListResponseDto create(@Valid @RequestBody ShoppingListRequestDto dto){return service.create(dto);}
 @PutMapping("/{id}") public ShoppingListResponseDto update(@PathVariable UUID id,@Valid @RequestBody ShoppingListRequestDto dto){return service.update(id,dto);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void cancel(@PathVariable UUID id){service.cancel(id);}
 @PostMapping("/{id}/items") @ResponseStatus(HttpStatus.CREATED) public ShoppingItemResponseDto add(@PathVariable UUID id,@Valid @RequestBody ShoppingItemRequestDto dto){return service.add(id,dto);}
 @PatchMapping("/{id}/items/{itemId}") public ShoppingItemResponseDto updateItem(@PathVariable UUID id,@PathVariable Integer itemId,@Valid @RequestBody ShoppingItemUpdateDto dto){return service.updateItem(id,itemId,dto);}
 @DeleteMapping("/{id}/items/{itemId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removeItem(@PathVariable UUID id,@PathVariable Integer itemId){service.removeItem(id,itemId);}
 @DeleteMapping("/{id}/purchased-items") @ResponseStatus(HttpStatus.NO_CONTENT) public void clearPurchased(@PathVariable UUID id){service.clearPurchased(id);}
 @PostMapping("/{id}/complete") public ShoppingListResponseDto complete(@PathVariable UUID id,@Valid @RequestBody CompleteShoppingListDto dto){return service.complete(id,dto);}
 @GetMapping("/suggestions") public List<ShoppingSuggestionDto> suggestions(@RequestParam Integer stockId){return service.suggestions(stockId);}
}
