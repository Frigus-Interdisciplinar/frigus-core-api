package com.frigus.coreapi.dto.shopping;

import com.frigus.coreapi.enums.ProductListStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListProductResponseDto {
    private Integer id;
    private UUID listId;
    private Integer productId;
    private ProductListStatus status;
    private Integer quantity;
}
