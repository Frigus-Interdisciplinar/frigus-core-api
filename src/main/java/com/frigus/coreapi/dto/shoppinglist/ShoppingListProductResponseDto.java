package com.frigus.coreapi.dto.shoppinglist;

import com.frigus.coreapi.enums.ProductListStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListProductResponseDto {
    private Integer id;
    private UUID listId;
    private Integer productId;
    private String productName;
    private String productCategory;
    private String productMeasure;
    private Integer quantity;
    private Double estimatedTotal;
    private ProductListStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
