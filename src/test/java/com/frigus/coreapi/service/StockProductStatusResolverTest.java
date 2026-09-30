package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.ProductStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StockProductStatusResolverTest {
    private final StockProductStatusResolver resolver = new StockProductStatusResolver();

    @Test
    void shouldResolveExpiredForPastDates() {
        assertThat(resolver.resolve(LocalDate.now().minusDays(1))).isEqualTo(ProductStatus.EXPIRED);
    }

    @Test
    void shouldResolveNearExpirationForTheNextSevenDays() {
        assertThat(resolver.resolve(LocalDate.now().plusDays(7))).isEqualTo(ProductStatus.NEAR_EXPIRATION);
    }

    @Test
    void shouldResolveFreshAfterNearExpirationWindow() {
        assertThat(resolver.resolve(LocalDate.now().plusDays(8))).isEqualTo(ProductStatus.FRESH);
    }
}
