package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.product.ProductCreateRequestDto;
import com.frigus.coreapi.dto.product.ProductResponseDto;
import com.frigus.coreapi.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper implements BaseMapper<Product, ProductResponseDto, ProductCreateRequestDto> {
    @Override
    public ProductResponseDto toDto(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName()).brand(product.getBrand()).imageUrl(product.getImageUrl())
                .ownerGroupId(product.getOwnerGroup()==null ? null : product.getOwnerGroup().getId())
                .category(product.getCategory())
                .storagePlace(product.getStoragePlace())
                .unitPrice(product.getUnitPrice())
                .unitOfMeasure(product.getUnitOfMeasure())
                .createdAt(product.getCreatedAt())
                .build();
    }

    @Override
    public Product toEntity(ProductCreateRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Product.builder()
                .name(dto.getName().trim()).brand(dto.getBrand()).imageUrl(dto.getImageUrl())
                .category(dto.getCategory())
                .storagePlace(dto.getStoragePlace())
                .unitPrice(dto.getUnitPrice())
                .unitOfMeasure(dto.getUnitOfMeasure())
                .build();
    }
}
