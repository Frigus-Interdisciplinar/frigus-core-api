package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.stock.StockCreateRequestDto;
import com.frigus.coreapi.dto.stock.StockResponseDto;
import com.frigus.coreapi.model.Stock;
import org.springframework.stereotype.Component;

@Component
public class StockMapper implements BaseMapper<Stock, StockResponseDto, StockCreateRequestDto>{

    @Override
    public StockResponseDto toDto(Stock stock) {
        return StockResponseDto.builder()
                .id(stock.getId())
                .groupId(stock.getGroup().getId())
                .name(stock.getName())
                .build();
    }

    @Override
    public Stock toEntity(StockCreateRequestDto dto) {
        return Stock.builder()
                .name(dto.getClass().getName())
                .build();
    }
}
