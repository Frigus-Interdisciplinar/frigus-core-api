package com.frigus.coreapi.mapper;

<<<<<<< HEAD
import com.frigus.coreapi.dto.shoppinglist.ShoppingListCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListResponseDto;
=======
import com.frigus.coreapi.dto.shopping.ShoppingListRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListResponseDto;
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
import com.frigus.coreapi.model.ShoppingList;
import org.springframework.stereotype.Component;

@Component
<<<<<<< HEAD
public class ShoppingListMapper implements BaseMapper<ShoppingList, ShoppingListResponseDto, ShoppingListCreateRequestDto> {

    @Override
    public ShoppingList toEntity(ShoppingListCreateRequestDto dto) {
        if (dto == null) {
            return null;
        }

        ShoppingList entity = new ShoppingList();
        entity.setDate(dto.getDate());
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
                .stockName(entity.getStock() != null ? entity.getStock().getName() : null)
                .date(entity.getDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
=======
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
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
                .build();
    }
}
