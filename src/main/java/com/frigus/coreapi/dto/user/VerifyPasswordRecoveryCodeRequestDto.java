package com.frigus.coreapi.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyPasswordRecoveryCodeRequestDto(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{6}")
        String code
) { }
