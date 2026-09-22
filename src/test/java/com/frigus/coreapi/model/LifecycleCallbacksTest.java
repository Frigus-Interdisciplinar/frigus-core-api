package com.frigus.coreapi.model;

import com.frigus.coreapi.enums.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LifecycleCallbacksTest {
    @Test
    void initializesPlanDefaultsAndUpdatesTimestamp() {
        Plan plan = Plan.builder().build();
        plan.prePersist();
        assertThat(plan.getActive()).isTrue();
        assertThat(plan.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(plan.getCreatedAt()).isNotNull();
        Instant beforeUpdate = plan.getUpdatedAt();
        plan.preUpdate();
        assertThat(plan.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    void initializesSubscriptionDefaultsAndUpdatesTimestamp() {
        Subscription subscription = Subscription.builder().build();
        subscription.prePersist();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(subscription.getStartedAt()).isNotNull();
        Instant beforeUpdate = subscription.getUpdatedAt();
        subscription.preUpdate();
        assertThat(subscription.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }
}
