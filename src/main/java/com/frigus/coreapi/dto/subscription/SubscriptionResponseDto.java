package com.frigus.coreapi.dto.subscription;

import com.frigus.coreapi.dto.plan.PlanLimitsDto;
import com.frigus.coreapi.dto.plan.PlanResponseDto;
import com.frigus.coreapi.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userName;
    private PlanResponseDto plan;
    private SubscriptionStatus status;
    private Instant startedAt;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant canceledAt;
    private PlanLimitsDto limits;
    private Boolean autoRenew;
}
