package com.frigus.coreapi.dto.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCreateRequestDto {
    @NotNull(message = "O grupo é obrigatório")
    private UUID groupId;

    @NotNull(message = "O nome é obrigatório")
    @Size(max = 2000, message = "O nome deve ter no máximo 2000 caracteres")
    private String name;
}
