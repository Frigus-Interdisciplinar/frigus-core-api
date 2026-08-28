package com.frigus.coreapi.model;

import com.frigus.coreapi.enums.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {
    @Test
    void returnsFreePlanDetailsUnlessThereIsAnActiveSubscription() {
        User user = User.builder().build();
        assertThat(user.getPlanNameFromUser()).isEqualTo("Frigus Free");
        assertThat(user.getPlanCodeFromUser()).isEqualTo("FREE");
        user.setSubscription(Subscription.builder().status(SubscriptionStatus.ACTIVE)
                .plan(Plan.builder().name("Frigus Família").planCode("FAMILY").build()).build());
        assertThat(user.getPlanNameFromUser()).isEqualTo("Frigus Família");
        assertThat(user.getPlanCodeFromUser()).isEqualTo("FAMILY");
    }
}
