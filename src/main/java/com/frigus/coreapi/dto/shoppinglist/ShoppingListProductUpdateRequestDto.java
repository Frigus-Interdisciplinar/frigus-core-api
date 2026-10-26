package com.frigus.coreapi.dto.shoppinglist;

import com.frigus.coreapi.enums.ProductListStatus;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListProductUpdateRequestDto {
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;
    private ProductListStatus status;
}
