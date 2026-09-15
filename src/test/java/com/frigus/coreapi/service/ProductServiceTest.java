package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.product.ProductCreateRequestDto;
import com.frigus.coreapi.dto.product.ProductUpdateRequestDto;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
import com.frigus.coreapi.enums.UnitOfMeasure;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.mapper.ProductMapper;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository productRepository;

    @Test
    void createsCatalogProduct() {
        ProductService service = service();
        when(productRepository.findFirstByNameIgnoreCase("Leite")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(8);
            return product;
        });

        var response = service.create(createRequest());

        ArgumentCaptor<Product> product = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(product.capture());
        assertThat(product.getValue().getCreatedAt()).isNotNull();
        assertThat(response.getId()).isEqualTo(8);
    }

    @Test
    void listsAllCatalogProducts() {
        PageRequest page = PageRequest.of(0, 10);
        ProductService service = service();
        when(productRepository.findAll(page)).thenReturn(new PageImpl<>(List.of(product(1), product(2))));

        var response = service.findAll(page);

        assertThat(response.getContent()).hasSize(2);
        verify(productRepository).findAll(page);
    }

    @Test
    void findsCatalogProductByIdThroughBaseService() {
        ProductService service = service();
        when(productRepository.findById(3)).thenReturn(Optional.of(product(3)));

        var response = service.findById(3);

        assertThat(response.getId()).isEqualTo(3);
        verify(productRepository).findById(3);
    }

    @Test
    void updatesCatalogProduct() {
        Product current = product(3);
        ProductUpdateRequestDto update = ProductUpdateRequestDto.builder()
                .name("  Iogurte  ")
                .category(Category.DAIRY)
                .storagePlace(StoragePlace.FRIDGE)
                .unitPrice(new BigDecimal("12.30"))
                .unitOfMeasure(UnitOfMeasure.UNIT)
                .build();
        ProductService service = service();
        when(productRepository.findById(3)).thenReturn(Optional.of(current));
        when(productRepository.findFirstByNameIgnoreCase("Iogurte")).thenReturn(Optional.empty());
        when(productRepository.save(current)).thenReturn(current);

        var response = service.update(3, update);

        assertThat(response.getName()).isEqualTo("Iogurte");
        assertThat(response.getUnitPrice()).isEqualByComparingTo("12.30");
        assertThat(response.getUnitOfMeasure()).isEqualTo(UnitOfMeasure.UNIT);
        verify(productRepository).save(current);
    }

    @Test
    void rejectsDuplicateNameDuringUpdate() {
        Product current = product(3);
        Product duplicate = product(4);
        ProductService service = service();
        when(productRepository.findById(3)).thenReturn(Optional.of(current));
        when(productRepository.findFirstByNameIgnoreCase("Leite")).thenReturn(Optional.of(duplicate));

        assertThatThrownBy(() -> service.update(3, updateRequest())).isInstanceOf(ConflictException.class);
    }

    @Test
    void preventsDeletingReferencedProduct() {
        ProductService service = service();
        when(productRepository.findById(3)).thenReturn(Optional.of(product(3)));
        when(productRepository.isReferenced(3)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(3)).isInstanceOf(ConflictException.class);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deletesUnreferencedProduct() {
        Product product = product(3);
        ProductService service = service();
        when(productRepository.findById(3)).thenReturn(Optional.of(product));
        when(productRepository.isReferenced(3)).thenReturn(false);

        service.delete(3);

        verify(productRepository).delete(product);
    }

    private ProductService service() {
        return new ProductService(productRepository, new ProductMapper());
    }

    private Product product(Integer id) {
        return Product.builder()
                .id(id)
                .name("Leite")
                .category(Category.DAIRY)
                .storagePlace(StoragePlace.FRIDGE)
                .unitPrice(new BigDecimal("8.50"))
                .unitOfMeasure(UnitOfMeasure.LITER)
                .build();
    }

    private ProductCreateRequestDto createRequest() {
        return ProductCreateRequestDto.builder()
                .name("Leite")
                .category(Category.DAIRY)
                .storagePlace(StoragePlace.FRIDGE)
                .unitPrice(new BigDecimal("8.50"))
                .unitOfMeasure(UnitOfMeasure.LITER)
                .build();
    }

    private ProductUpdateRequestDto updateRequest() {
        return ProductUpdateRequestDto.builder()
                .name("Leite")
                .category(Category.DAIRY)
                .storagePlace(StoragePlace.FRIDGE)
                .unitPrice(new BigDecimal("8.50"))
                .unitOfMeasure(UnitOfMeasure.LITER)
                .build();
    }
}
