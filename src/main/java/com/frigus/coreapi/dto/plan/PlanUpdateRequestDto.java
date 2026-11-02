package com.frigus.coreapi.dto.plan;

import com.frigus.coreapi.enums.BillingInterval;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanUpdateRequestDto {

    private String name;

    private String description;

    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
    private BigDecimal price;

    private BillingInterval billingInterval;

    private Boolean active;
}
