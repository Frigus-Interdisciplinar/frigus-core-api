package com.frigus.coreapi.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChangePasswordRequestDto {

    @NotBlank(message = "A senha atual é obrigatória")
    private String oldPassword;

    @NotBlank(message = "A nova senha é obrigatória")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,20}$",
        message = "A nova senha deve conter de 8 a 20 caracteres, contendo pelo menos uma letra maiúscula, uma letra minúscula, um número e um caractere especial"
    )
    private String newPassword;
}
