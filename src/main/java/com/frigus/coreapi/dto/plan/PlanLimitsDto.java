package com.frigus.coreapi.dto.plan;

import java.math.BigDecimal;

import com.frigus.coreapi.enums.PlanCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlanLimitsDto {
    private PlanCode planCode;

    private int maxGroupMembers;
    private int maxStocks;
    private int maxProductsPerStock;

    private boolean allowOwnProducts;
    private boolean allowSavedRecipes;
    private boolean allowMoneySaving;
    
    private boolean isEnterprise;
    private BigDecimal costPerPublishedAd;
    private boolean hasMonthlyReport;
}
