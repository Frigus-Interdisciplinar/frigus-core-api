package com.frigus.coreapi.dto.stockproduct;

import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
import com.frigus.coreapi.enums.UnitOfMeasure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockProductProductResponseDto {
    private Integer id;
    private String name;
    private Category category;
    private StoragePlace storagePlace;
    private BigDecimal unitPrice;
    private UnitOfMeasure unitOfMeasure;
    private String brand;
    private String imageUrl;
}
