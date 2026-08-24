package com.frigus.coreapi.dto.shopping;

import com.frigus.coreapi.enums.ProductListStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ShoppingListProductRequestDto {
    @NotNull
    private UUID listId;

    @NotNull
    private Integer productId;

    private ProductListStatus status;

    @NotNull
    @Positive
    @Builder.Default
    private Integer quantity = 1;
}
