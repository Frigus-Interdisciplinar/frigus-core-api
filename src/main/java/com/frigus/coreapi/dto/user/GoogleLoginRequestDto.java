package com.frigus.coreapi.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequestDto {
    @NotBlank(message = "O token do Google é obrigatório")
    @Size(max = 8192, message = "O token do Google é inválido")
    private String idToken;
}
