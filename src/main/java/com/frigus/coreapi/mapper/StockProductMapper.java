package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.stockproduct.StockProductCreateRequestDto;
import com.frigus.coreapi.dto.stockproduct.StockProductResponseDto;
import com.frigus.coreapi.dto.stockproduct.StockProductProductResponseDto;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.StockProduct;
import org.springframework.stereotype.Component;

@Component
public class StockProductMapper implements BaseMapper<StockProduct, StockProductResponseDto, StockProductCreateRequestDto> {
    @Override
    public StockProductResponseDto toDto(StockProduct stockProduct) {
        Product product = stockProduct.getProduct();
        return StockProductResponseDto.builder()
                .id(stockProduct.getId())
                .productId(product.getId())
                .product(StockProductProductResponseDto.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .category(product.getCategory())
                        .storagePlace(product.getStoragePlace())
                        .unitPrice(product.getUnitPrice())
                        .unitOfMeasure(product.getUnitOfMeasure())
                        .brand(product.getBrand())
                        .imageUrl(product.getImageUrl())
                        .build())
                .stockId(stockProduct.getStock().getId())
                .quantity(stockProduct.getQuantity())
                .minimalQuantity(stockProduct.getMinimalQuantity())
                .purchaseUnitPrice(stockProduct.getPurchaseUnitPrice())
                .expireDate(stockProduct.getExpireDate())
                .productStatus(stockProduct.getProductStatus())
                .category(stockProduct.getCategory())
                .build();
    }

    @Override
    public StockProduct toEntity(StockProductCreateRequestDto dto) {
        return StockProduct.builder()
                .quantity(dto.getQuantity())
                .minimalQuantity(dto.getMinimalQuantity())
                .purchaseUnitPrice(dto.getPurchaseUnitPrice())
                .expireDate(dto.getExpireDate())
                .build();
    }
}
