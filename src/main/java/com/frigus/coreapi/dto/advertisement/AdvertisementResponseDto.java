package com.frigus.coreapi.dto.advertisement;

import com.frigus.coreapi.enums.AdvertisementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdvertisementResponseDto(
        UUID id,
        UUID advertiserId,
        String advertiserName,
        String name,
        String productName,
        String category,
        String description,
        String imageUrl,
        String destinationUrl,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        AdvertisementStatus status,
        long impressions,
        long clicks,
        Instant createdAt,
        Instant updatedAt
) { }
