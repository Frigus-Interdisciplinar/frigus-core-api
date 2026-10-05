package com.frigus.coreapi.mapper;

<<<<<<< HEAD
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductResponseDto;
=======
import com.frigus.coreapi.dto.shopping.ShoppingListProductRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListProductResponseDto;
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
import com.frigus.coreapi.model.ShoppingListProduct;
import org.springframework.stereotype.Component;

@Component
<<<<<<< HEAD
public class ShoppingListProductMapper implements BaseMapper<ShoppingListProduct, ShoppingListProductResponseDto, ShoppingListProductCreateRequestDto> {

    @Override
    public ShoppingListProduct toEntity(ShoppingListProductCreateRequestDto dto) {
        if (dto == null) {
            return null;
        }

        ShoppingListProduct entity = new ShoppingListProduct();
        entity.setQuantity(dto.getQuantity());
        return entity;
    }

    @Override
    public ShoppingListProductResponseDto toDto(ShoppingListProduct entity) {
        if (entity == null) {
            return null;
        }

        Double estimatedTotal = 0.0;
        if (entity.getProduct() != null && entity.getProduct().getUnitPrice() != null && entity.getQuantity() != null) {
            estimatedTotal = entity.getProduct().getUnitPrice().doubleValue() * entity.getQuantity();
        }

        return ShoppingListProductResponseDto.builder()
                .id(entity.getId())
                .listId(entity.getList() != null ? entity.getList().getId() : null)
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .productName(entity.getProduct() != null ? entity.getProduct().getName() : null)
                .productCategory(entity.getProduct() != null && entity.getProduct().getCategory() != null ? entity.getProduct().getCategory().name() : null)
                .productMeasure(entity.getProduct() != null && entity.getProduct().getUnitOfMeasure() != null ? entity.getProduct().getUnitOfMeasure().name() : null)
                .quantity(entity.getQuantity())
                .estimatedTotal(estimatedTotal)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
=======
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
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
                .build();
    }
}
