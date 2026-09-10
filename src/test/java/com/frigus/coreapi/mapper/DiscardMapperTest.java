package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.discard.DiscardCreateRequestDto;
import com.frigus.coreapi.dto.discard.DiscardResponseDto;
import com.frigus.coreapi.model.Discard;
import com.frigus.coreapi.model.StockProduct;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DiscardMapperTest {

    private final DiscardMapper mapper = new DiscardMapper();

    @Test
    void shouldMapEntityToResponseDto() {
        Instant date = Instant.parse("2026-01-01T10:00:00Z");
        StockProduct stockProduct = StockProduct.builder().id(12).build();
        Discard discard = Discard.builder()
                .id(3)
                .stockProduct(stockProduct)
                .reason("Produto vencido")
                .date(date)
                .build();

        DiscardResponseDto response = mapper.toDto(discard);

        assertEquals(3, response.getId());
        assertEquals(12, response.getStockProductId());
        assertEquals("Produto vencido", response.getReason());
        assertEquals(date, response.getDate());
    }

    @Test
    void shouldMapCreateRequestToEntityWithoutPersistenceFields() {
        DiscardCreateRequestDto request = DiscardCreateRequestDto.builder()
                .stockProductId(12)
                .reason("Produto danificado")
                .build();

        Discard discard = mapper.toEntity(request);

        assertNull(discard.getId());
        assertNull(discard.getStockProduct());
        assertNull(discard.getDate());
        assertEquals("Produto danificado", discard.getReason());
    }
}
