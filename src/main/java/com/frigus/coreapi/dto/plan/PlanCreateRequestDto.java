package com.frigus.coreapi.dto.plan;

import com.frigus.coreapi.enums.BillingInterval;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreateRequestDto {

    @NotBlank(message = "O código do plano é obrigatório")
    @Size(max = 20, message = "O código do plano deve ter no máximo 20 caracteres")
    private String planCode;

    @NotBlank(message = "O nome do plano é obrigatório")
    private String name;

    private String description;

    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
    private BigDecimal price;

    private BillingInterval billingInterval;

    @Builder.Default
    private Boolean active = true;
}
