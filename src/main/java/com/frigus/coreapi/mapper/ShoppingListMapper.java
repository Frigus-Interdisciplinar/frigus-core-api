package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.shoppinglist.ShoppingListCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListResponseDto;
import com.frigus.coreapi.model.ShoppingList;
import org.springframework.stereotype.Component;

@Component
public class ShoppingListMapper implements BaseMapper<ShoppingList, ShoppingListResponseDto, ShoppingListCreateRequestDto> {

    @Override
    public ShoppingList toEntity(ShoppingListCreateRequestDto dto) {
        if (dto == null) {
            return null;
        }

        ShoppingList entity = new ShoppingList();
        entity.setDate(dto.getDate());
        if (dto.getName() != null && !dto.getName().isBlank()) {
            entity.setName(dto.getName().trim());
        }
        return entity;
    }

    @Override
    public ShoppingListResponseDto toDto(ShoppingList entity) {
        if (entity == null) {
            return null;
        }

        return ShoppingListResponseDto.builder()
                .id(entity.getId())
                .stockId(entity.getStock() != null ? entity.getStock().getId() : null)
                .name(entity.getName())
                .stockName(entity.getStock() != null ? entity.getStock().getName() : null)
                .date(entity.getDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
