package com.frigus.coreapi.dto.stockmovement;

import com.frigus.coreapi.enums.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponseDto {
    private Integer id;
    private Integer stockProductId;
    private UUID userId;
    private MovementType movementType;
    private Integer quantity;
    private Integer balanceAfter;
    private Instant date;
}
