package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.discard.DiscardCreateRequestDto;
import com.frigus.coreapi.dto.discard.DiscardResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.DiscardMapper;
import com.frigus.coreapi.model.Discard;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.repository.DiscardRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscardServiceTest {

    @Mock
    private DiscardRepository discardRepository;

    @Mock
    private DiscardMapper discardMapper;

    @Mock
    private StockProductRepository stockProductRepository;

    @InjectMocks
    private DiscardService discardService;

    @Test
    void shouldCreateDiscardUsingExistingStockProduct() {
        DiscardCreateRequestDto request = DiscardCreateRequestDto.builder()
                .stockProductId(12)
                .reason("Produto vencido")
                .build();
        StockProduct stockProduct = StockProduct.builder().id(12).build();
        Discard discard = Discard.builder().reason(request.getReason()).build();
        Discard savedDiscard = Discard.builder()
                .id(3)
                .stockProduct(stockProduct)
                .reason(request.getReason())
                .build();
        DiscardResponseDto response = DiscardResponseDto.builder().id(3).stockProductId(12).build();

        when(stockProductRepository.findById(12)).thenReturn(Optional.of(stockProduct));
        when(discardMapper.toEntity(request)).thenReturn(discard);
        when(discardRepository.save(discard)).thenReturn(savedDiscard);
        when(discardMapper.toDto(savedDiscard)).thenReturn(response);

        DiscardResponseDto result = discardService.create(request);

        assertEquals(3, result.getId());
        assertEquals(12, result.getStockProductId());
        assertEquals(stockProduct, discard.getStockProduct());
        assertNotNull(discard.getDate());
        verify(discardRepository).save(discard);
    }

    @Test
    void shouldRejectCreateWhenStockProductDoesNotExist() {
        DiscardCreateRequestDto request = DiscardCreateRequestDto.builder()
                .stockProductId(999)
                .reason("Produto vencido")
                .build();
        when(stockProductRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> discardService.create(request));
    }
}
