package com.frigus.coreapi.dto.stockproduct;

import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockProductResponseDto {
    private Integer id;
    private Integer productId;
    private Integer stockId;
    private Integer quantity;
    private Integer minimalQuantity;
    private LocalDate expireDate;
    private ProductStatus productStatus;
    private Category category;
}
