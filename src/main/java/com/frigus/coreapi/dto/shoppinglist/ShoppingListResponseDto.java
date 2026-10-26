package com.frigus.coreapi.dto.shoppinglist;

import com.frigus.coreapi.enums.ListStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListResponseDto {
    private UUID id;
    private Integer stockId;
    private String stockName;
    private LocalDate date;
    private ListStatus status;
    private Long totalItems;
    private Long purchasedItems;
    private Long pendingItems;
    private Double estimatedTotal;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ShoppingListProductResponseDto> items;
}
