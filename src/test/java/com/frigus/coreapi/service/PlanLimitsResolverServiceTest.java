package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.PlanCode;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanLimitsResolverServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private PlanLimitsResolverService service;

    @Test
    void resolvesEachConfiguredPlanLimit() {
        assertThat(service.getLimitsForPlan(PlanCode.FREE).getMaxStocks()).isEqualTo(1);
        assertThat(service.getLimitsForPlan(PlanCode.PLUS).getMaxStocks()).isEqualTo(3);
        assertThat(service.getLimitsForPlan(PlanCode.FAMILY).getMaxGroupMembers()).isEqualTo(10);
        assertThat(service.getLimitsForPlan(PlanCode.COMMERCIAL).getMaxProductsPerStock()).isEqualTo(200);
        var enterprise = service.getLimitsForPlan(PlanCode.ENTERPRISE);
        assertThat(enterprise.isEnterprise()).isTrue();
        assertThat(enterprise.getCostPerPublishedAd()).isEqualByComparingTo("0.99");
    }

    @Test
    void resolvesLimitsForPersistedUsersAndRejectsMissingUsers() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).subscription(Subscription.builder()
                .plan(Plan.builder().planCode("PLUS").build()).build()).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        assertThat(service.getLimitsForUser(id).getPlanCode()).isEqualTo(PlanCode.PLUS);
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLimitsForUser(missing)).isInstanceOf(NotFoundException.class);
    }
}
