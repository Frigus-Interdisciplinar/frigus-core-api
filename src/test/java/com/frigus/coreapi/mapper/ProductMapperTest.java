package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.product.ProductCreateRequestDto;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
import com.frigus.coreapi.enums.UnitOfMeasure;
import com.frigus.coreapi.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {
    private final ProductMapper mapper = new ProductMapper();

    @Test
    void mapsProductAndNormalizesCreationInput() {
        Product product = Product.builder()
                .id(7)
                .name("Leite")
                .category(Category.DAIRY)
                .storagePlace(StoragePlace.FRIDGE)
                .unitPrice(new BigDecimal("8.50"))
                .unitOfMeasure(UnitOfMeasure.LITER)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        assertThat(mapper.toDto(product).getName()).isEqualTo("Leite");
        assertThat(mapper.toDto(null)).isNull();

        Product entity = mapper.toEntity(ProductCreateRequestDto.builder()
                .name("  Leite  ")
                .category(Category.DAIRY)
                .storagePlace(StoragePlace.FRIDGE)
                .unitPrice(new BigDecimal("8.50"))
                .unitOfMeasure(UnitOfMeasure.LITER)
                .build());

        assertThat(entity.getName()).isEqualTo("Leite");
        assertThat(entity.getUnitOfMeasure()).isEqualTo(UnitOfMeasure.LITER);
        assertThat(mapper.toEntity(null)).isNull();
    }
}
