package com.frigus.coreapi.dto.plan;

import com.frigus.coreapi.enums.BillingInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponseDto {
    private Integer id;
    private String planCode;
    private String name;
    private String description;
    private BigDecimal price;
    private BillingInterval billingInterval;
    private Boolean active;
    private PlanLimitsDto limits;
    private Instant createdAt;
    private Instant updatedAt;
}
