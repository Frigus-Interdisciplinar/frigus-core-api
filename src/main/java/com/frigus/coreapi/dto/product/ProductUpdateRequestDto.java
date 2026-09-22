package com.frigus.coreapi.dto.product;

import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
import com.frigus.coreapi.enums.UnitOfMeasure;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequestDto {
    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
    private String name;

    @NotNull(message = "A categoria é obrigatória")
    private Category category;

    @NotNull(message = "O local de armazenamento é obrigatório")
    private StoragePlace storagePlace;

    @NotNull(message = "O preço unitário é obrigatório")
    @DecimalMin(value = "0.00", inclusive = false, message = "O preço unitário deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "O preço unitário deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal unitPrice;

    @NotNull(message = "A unidade de medida é obrigatória")
    private UnitOfMeasure unitOfMeasure;
}
