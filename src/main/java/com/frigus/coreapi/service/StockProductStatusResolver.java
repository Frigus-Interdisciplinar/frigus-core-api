package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.ProductStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StockProductStatusResolver {
    private static final int NEAR_EXPIRATION_DAYS = 7;

    public ProductStatus resolve(LocalDate expireDate) {
        LocalDate today = LocalDate.now();
        if (expireDate.isBefore(today)) {
            return ProductStatus.EXPIRED;
        }
        if (!expireDate.isAfter(today.plusDays(NEAR_EXPIRATION_DAYS))) {
            return ProductStatus.NEAR_EXPIRATION;
        }
        return ProductStatus.FRESH;
    }
}
