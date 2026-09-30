package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.plan.PlanCreateRequestDto;
import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.enums.BillingInterval;
import com.frigus.coreapi.enums.PlanCode;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.service.PlanLimitsResolverService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlanMapperTest {
    @Test
    void mapsPlanDataAndResolvedLimitsAndHandlesNullInputs() {
        PlanLimitsResolverService resolver = mock(PlanLimitsResolverService.class);
        PlanMapper mapper = new PlanMapper(resolver);
        PlanLimitsDto limits = PlanLimitsDto.builder().planCode(PlanCode.PLUS).maxStocks(3).build();
        when(resolver.getLimitsForPlan(PlanCode.PLUS)).thenReturn(limits);
        Plan plan = Plan.builder().id(1).planCode("plus").name("Plus").price(new BigDecimal("9.99")).active(true).build();
        var response = mapper.toDto(plan);
        assertThat(response.getLimits()).isSameAs(limits);
        assertThat(response.getPlanCode()).isEqualTo("plus");
        assertThat(mapper.toDto(null)).isNull();

        var entity = mapper.toEntity(PlanCreateRequestDto.builder().planCode(" plus ").name(" Plus ")
                .price(new BigDecimal("9.99")).billingInterval(BillingInterval.MONTHLY).build());
        assertThat(entity.getPlanCode()).isEqualTo("PLUS");
        assertThat(entity.getName()).isEqualTo("Plus");
        assertThat(entity.getActive()).isTrue();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
