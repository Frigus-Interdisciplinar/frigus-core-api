package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.shopping.ShoppingListProductRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListProductResponseDto;
import com.frigus.coreapi.model.ShoppingListProduct;
import org.springframework.stereotype.Component;

@Component
public class ShoppingListProductMapper implements BaseMapper<ShoppingListProduct, ShoppingListProductResponseDto, ShoppingListProductRequestDto> {
    @Override
    public ShoppingListProductResponseDto toDto(ShoppingListProduct model) {
        return ShoppingListProductResponseDto.builder()
                .id(model.getId())
                .listId(model.getList().getId())
                .productId(model.getProduct().getId())
                .status(model.getStatus())
                .quantity(model.getQuantity())
                .build();
    }

    @Override
    public ShoppingListProduct toEntity(ShoppingListProductRequestDto dto) {
        return ShoppingListProduct.builder()
                .status(dto.getStatus() == null ? com.frigus.coreapi.enums.ProductListStatus.PENDING : dto.getStatus())
                .quantity(dto.getQuantity() == null ? 1 : dto.getQuantity())
                .build();
    }
}
