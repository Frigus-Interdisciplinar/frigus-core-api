package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.stockproduct.StockProductCreateRequestDto;
import com.frigus.coreapi.dto.stockproduct.StockProductResponseDto;
import com.frigus.coreapi.model.StockProduct;
import org.springframework.stereotype.Component;

@Component
@lombok.RequiredArgsConstructor
public class StockProductMapper implements BaseMapper<StockProduct, StockProductResponseDto, StockProductCreateRequestDto> {
    private final ProductMapper productMapper;

    @Override
    public StockProductResponseDto toDto(StockProduct stockProduct) {
        return StockProductResponseDto.builder()
                .id(stockProduct.getId())
                .productId(stockProduct.getProduct().getId()).product(productMapper.toDto(stockProduct.getProduct())).batch(stockProduct.getBatch())
                .stockId(stockProduct.getStock().getId())
                .quantity(stockProduct.getQuantity())
                .minimalQuantity(stockProduct.getMinimalQuantity())
                .expireDate(stockProduct.getExpireDate())
                .productStatus(stockProduct.getProductStatus())
                .category(stockProduct.getCategory())
                .build();
    }

    @Override
    public StockProduct toEntity(StockProductCreateRequestDto dto) {
        return StockProduct.builder()
                .quantity(dto.getQuantity()).batch(dto.getBatch()==null || dto.getBatch().isBlank() ? null : dto.getBatch().trim())
                .minimalQuantity(dto.getMinimalQuantity())
                .expireDate(dto.getExpireDate())
                .build();
    }
}
