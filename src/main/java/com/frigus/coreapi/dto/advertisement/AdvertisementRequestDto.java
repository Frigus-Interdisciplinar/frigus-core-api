package com.frigus.coreapi.dto.advertisement;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdvertisementRequestDto(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 160) String productName,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 2000) String description,
        @NotBlank @Size(max = 2048) @URL(protocol = "https") String imageUrl,
        @Size(max = 2048) @URL(protocol = "https") String destinationUrl,
        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        LocalDate startDate,
        LocalDate endDate
) { }
