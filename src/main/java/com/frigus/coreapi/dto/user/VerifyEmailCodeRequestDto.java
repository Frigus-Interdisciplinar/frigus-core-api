package com.frigus.coreapi.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailCodeRequestDto(
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {}
