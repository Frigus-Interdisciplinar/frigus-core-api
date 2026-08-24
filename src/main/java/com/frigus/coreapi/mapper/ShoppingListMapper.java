package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.shopping.ShoppingListRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListResponseDto;
import com.frigus.coreapi.model.ShoppingList;
import org.springframework.stereotype.Component;

@Component
public class ShoppingListMapper implements BaseMapper<ShoppingList, ShoppingListResponseDto, ShoppingListRequestDto> {
    @Override
    public ShoppingListResponseDto toDto(ShoppingList model) {
        return ShoppingListResponseDto.builder()
                .id(model.getId())
                .date(model.getDate())
                .stockId(model.getStock().getId())
                .status(model.getStatus())
                .createdAt(model.getCreatedAt())
                .build();
    }

    @Override
    public ShoppingList toEntity(ShoppingListRequestDto dto) {
        return ShoppingList.builder()
                .date(dto.getDate() == null ? java.time.LocalDate.now() : dto.getDate())
                .status(dto.getStatus() == null ? com.frigus.coreapi.enums.ListStatus.OPEN : dto.getStatus())
                .build();
    }
}
