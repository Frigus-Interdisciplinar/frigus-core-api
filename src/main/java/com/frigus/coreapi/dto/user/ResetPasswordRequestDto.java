package com.frigus.coreapi.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{6}")
        String code,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,20}$",
                message = "A senha deve conter de 8 a 20 caracteres, "
                        + "contendo pelo menos uma letra maiúscula, uma letra minúscula, "
                        + "um número e um caractere especial"
        )
        String newPassword
) { }
