package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.dto.plan.PlanResponseDto;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SubscriptionMapperTest {
    @Test
    void mapsSubscriptionAndComputesAutoRenewalState() {
        PlanMapper planMapper = mock(PlanMapper.class);
        SubscriptionMapper mapper = new SubscriptionMapper(planMapper);
        PlanLimitsDto limits = PlanLimitsDto.builder().maxStocks(3).build();
        PlanResponseDto plan = PlanResponseDto.builder().limits(limits).build();
        Subscription subscription = Subscription.builder().id(UUID.randomUUID()).status(SubscriptionStatus.ACTIVE)
                .user(User.builder().id(UUID.randomUUID()).name("Ana").email("ana@frigus.com").build()).build();
        when(planMapper.toDto(null)).thenReturn(plan);
        var response = mapper.toDto(subscription);
        assertThat(response.getUserEmail()).isEqualTo("ana@frigus.com");
        assertThat(response.getLimits()).isSameAs(limits);
        assertThat(response.getAutoRenew()).isTrue();
        subscription.setCanceledAt(java.time.Instant.now());
        assertThat(mapper.toDto(subscription).getAutoRenew()).isFalse();
        assertThat(mapper.toDto(null)).isNull();
    }
}
