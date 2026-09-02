package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.stockproduct.StockProductCreateRequestDto;
import com.frigus.coreapi.dto.stockproduct.StockProductResponseDto;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.ProductStatus;
import com.frigus.coreapi.mapper.StockProductMapper;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import com.frigus.coreapi.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockProductServiceTest {
    @Mock private StockProductRepository stockProductRepository;
    @Mock private StockRepository stockRepository;
    @Mock private ProductRepository productRepository;
    @Mock private GroupAccessService groupAccessService;

    @Test
    void shouldCreateStockProductWithCategoryAndDerivedStatus() {
        UUID groupId = UUID.randomUUID();
        Stock stock = Stock.builder()
                .id(3)
                .group(Group.builder().id(groupId).build())
                .build();
        Product product = Product.builder().id(7).category(Category.DAIRY).build();
        StockProductCreateRequestDto request = StockProductCreateRequestDto.builder()
                .stockId(3)
                .productId(7)
                .quantity(4)
                .minimalQuantity(1)
                .expireDate(LocalDate.now().plusDays(2))
                .build();
        StockProductService service = new StockProductService(
                stockProductRepository,
                new StockProductMapper(),
                stockRepository,
                productRepository,
                groupAccessService,
                new StockProductStatusResolver());

        when(stockRepository.findById(3)).thenReturn(Optional.of(stock));
        when(productRepository.findById(7)).thenReturn(Optional.of(product));
        when(stockProductRepository.existsByProductIdAndStockIdAndExpireDate(7, 3, request.getExpireDate()))
                .thenReturn(false);
        when(stockProductRepository.save(org.mockito.ArgumentMatchers.any(StockProduct.class)))
                .thenAnswer(invocation -> {
                    StockProduct saved = invocation.getArgument(0);
                    saved.setId(11);
                    return saved;
                });

        StockProductResponseDto response = service.create(request);

        assertThat(response.getId()).isEqualTo(11);
        assertThat(response.getCategory()).isEqualTo(Category.DAIRY);
        assertThat(response.getProductStatus()).isEqualTo(ProductStatus.NEAR_EXPIRATION);
        assertThat(response.getQuantity()).isEqualTo(4);
    }
}
