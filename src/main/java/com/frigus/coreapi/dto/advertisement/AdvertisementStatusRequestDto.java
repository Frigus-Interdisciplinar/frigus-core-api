package com.frigus.coreapi.dto.advertisement;

import com.frigus.coreapi.enums.AdvertisementStatus;
import jakarta.validation.constraints.NotNull;

public record AdvertisementStatusRequestDto(@NotNull AdvertisementStatus status) { }
