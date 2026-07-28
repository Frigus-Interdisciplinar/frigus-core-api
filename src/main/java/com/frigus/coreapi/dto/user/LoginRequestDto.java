package com.frigus.coreapi.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequestDto {
    @NotBlank(message = "Email deve ser preenchido")
    @Email(message = "Email invalido")
    private String email;

    @NotBlank(message = "Senha deve ser preenchida")
    private String rawPassword;
}