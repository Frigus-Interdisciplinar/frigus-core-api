package com.frigus.coreapi.dto.shoppinglist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListCreateRequestDto {
    @NotNull(message = "Stock ID is required")
    private Integer stockId;

    private LocalDate date; // Optional planned date

    @Valid
    private List<ShoppingListProductCreateRequestDto> initialItems;
}
