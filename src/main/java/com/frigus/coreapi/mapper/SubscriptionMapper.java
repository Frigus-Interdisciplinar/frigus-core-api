package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.plan.PlanResponseDto;
import com.frigus.coreapi.dto.subscription.SubscriptionResponseDto;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionMapper {

    private final PlanMapper planMapper;

    public SubscriptionResponseDto toDto(Subscription model) {
        if (model == null) {
            return null;
        }

        PlanResponseDto planDto = planMapper.toDto(model.getPlan());
        boolean autoRenew = model.getStatus() == SubscriptionStatus.ACTIVE && model.getCanceledAt() == null;

        return SubscriptionResponseDto.builder()
                .id(model.getId())
                .userId(model.getUser() != null ? model.getUser().getId() : null)
                .userEmail(model.getUser() != null ? model.getUser().getEmail() : null)
                .userName(model.getUser() != null ? model.getUser().getName() : null)
                .plan(planDto)
                .status(model.getStatus())
                .startedAt(model.getStartedAt())
                .currentPeriodStart(model.getCurrentPeriodStart())
                .currentPeriodEnd(model.getCurrentPeriodEnd())
                .canceledAt(model.getCanceledAt())
                .limits(planDto != null ? planDto.getLimits() : null)
                .autoRenew(autoRenew)
                .build();
    }
}
