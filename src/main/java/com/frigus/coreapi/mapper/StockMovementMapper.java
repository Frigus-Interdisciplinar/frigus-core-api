package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.stockmovement.StockMovementCreateRequestDto;
import com.frigus.coreapi.dto.stockmovement.StockMovementResponseDto;
import com.frigus.coreapi.model.StockMovement;
import org.springframework.stereotype.Component;

@Component
public class StockMovementMapper implements BaseMapper<StockMovement, StockMovementResponseDto, StockMovementCreateRequestDto> {
    @Override
    public StockMovementResponseDto toDto(StockMovement movement) {
        return StockMovementResponseDto.builder()
                .id(movement.getId())
                .stockProductId(movement.getStockProduct().getId())
                .userId(movement.getUser().getId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .balanceAfter(movement.getBalanceAfter())
                .date(movement.getDate())
                .build();
    }

    @Override
    public StockMovement toEntity(StockMovementCreateRequestDto dto) {
        return StockMovement.builder()
                .movementType(dto.getMovementType())
                .quantity(dto.getQuantity())
                .build();
    }
}
